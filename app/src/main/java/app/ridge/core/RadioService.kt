package app.ridge.core

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Network
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import app.ridge.MainActivity
import app.ridge.RidgeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RadioService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    private lateinit var router: AudioRouter
    private lateinit var discovery: Discovery
    private lateinit var vox: VoxRecorder
    private lateinit var transport: VoiceTransport
    private val store get() = RidgeStore.Instance

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisor)
    private var modeJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var transportRunning = false
    private var audioModeActive = false

    override fun onCreate() {
        super.onCreate()
        router = AudioRouter(this)
        discovery = Discovery(this)
        vox = VoxRecorder(
            ctx = this,
            onLevel = { lvl -> store.setVoxLevel((lvl * 100).toInt()) },
            onActiveChanged = { active ->
                store.setTransmitting(active)
                updateNotif()
            },
        )
        transport = VoiceTransport(
            ctx = this,
            onLevel = { lvl -> store.setVoxLevel((lvl * 100).toInt()) },
            onActiveChanged = { active ->
                store.setTransmitting(active)
                updateNotif()
            },
            onPeerCount = { n -> store.setPeerCount(n); updateNotif() },
            onError = { e -> store.setLastError(e) },
            onStats = { tx, rx -> store.setStats(tx, rx) },
        )
        startForegroundTyped()
        discovery.start()
        observeLifecycle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // User asked to fully stop the radio.
            store.leaveGroup()
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundTyped()
        return START_STICKY
    }

    /** Start as a typed foreground service so the mic works in the background.
     *  Typed FGS exists from API 29 (Q); on older we use the untyped form. */
    private fun startForegroundTyped() {
        val notif = buildNotif()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                startForeground(
                    NOTIF_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            }.onFailure {
                // Some OEMs reject CONNECTED_DEVICE; retry with microphone only.
                runCatching {
                    startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                }.onFailure { startForeground(NOTIF_ID, notif) }
            }
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun observeLifecycle() {
        modeJob?.cancel()
        modeJob = scope.launch {
            // 1) Start / stop transport based on role + connection state
            store.state
                .map {
                    TransportTrigger(
                        hasGroup = it.hasGroup,
                        role = it.role,
                        hotspotActive = it.hotspotState is HotspotState.Active,
                        joinConnected = it.joinState is JoinState.Connected,
                        hostIp = (it.joinState as? JoinState.Connected)?.gatewayIp,
                        joinNetwork = (it.joinState as? JoinState.Connected)?.network,
                    )
                }
                .distinctUntilChanged()
                .onEach { trig ->
                    try {
                        val wantTransport = trig.hasGroup && (
                                (trig.role == Role.Host && trig.hotspotActive) ||
                                        (trig.role == Role.Joiner && trig.joinConnected)
                                )
                        if (wantTransport && !transportRunning) {
                            // hand mic over from preview VOX, give it a moment to release
                            vox.stop()
                            Thread.sleep(120)
                            enterCommunicationAudio()
                            when (trig.role) {
                                Role.Host -> transport.startHost(network = null)
                                Role.Joiner -> transport.startClient(
                                    trig.hostIp ?: "192.168.43.1",
                                    trig.joinNetwork,
                                )
                                else -> Unit
                            }
                            transportRunning = true
                            store.setTransportActive(true)
                        } else if (!wantTransport && transportRunning) {
                            transport.stop()
                            transportRunning = false
                            store.setTransportActive(false)
                            exitCommunicationAudio()
                        }
                        updateNotif()
                    } catch (e: Throwable) {
                        store.setLastError("transport: ${e.message}")
                        transportRunning = false
                        store.setTransportActive(false)
                    }
                }
                .launchIn(this)

            // 2) Drive transmit based on talk mode + transmitting flag + transport state
            store.state
                .map {
                    TransmitTrigger(
                        transportActive = it.transportActive,
                        hasGroup = it.hasGroup,
                        talkMode = it.talkMode,
                        transmitting = it.transmitting,
                    )
                }
                .distinctUntilChanged()
                .onEach { t ->
                    try {
                        if (t.transportActive) {
                            when (t.talkMode) {
                                TalkMode.Hold -> {
                                    transport.setVoxEnabled(false)
                                    transport.setHoldPressed(t.transmitting)
                                }
                                TalkMode.HandsFree -> {
                                    transport.setVoxEnabled(t.hasGroup)
                                    transport.setHoldPressed(false)
                                }
                            }
                            if (t.transmitting) acquireWake() else releaseWake()
                        } else {
                            // pre-connect VOX preview when in HandsFree but transport not active
                            val voxWant = t.hasGroup && t.talkMode == TalkMode.HandsFree
                            if (voxWant) {
                                val started = vox.start()
                                if (started) acquireWake() else releaseWake()
                            } else {
                                vox.stop()
                                if (t.transmitting) acquireWake() else releaseWake()
                            }
                        }
                        updateNotif()
                    } catch (e: Throwable) {
                        store.setLastError("transmit: ${e.message}")
                    }
                }
                .launchIn(this)
        }
    }

    private fun enterCommunicationAudio() {
        if (audioModeActive) return
        runCatching { router.apply(store.state.value.audioOut) }
        audioModeActive = true
    }

    private fun exitCommunicationAudio() {
        if (!audioModeActive) return
        runCatching { router.release() }
        audioModeActive = false
    }

    private fun acquireWake() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ridge:radio").also {
            runCatching { it.acquire(60 * 60 * 1000L) }
        }
    }

    private fun releaseWake() {
        wakeLock?.let { wl ->
            runCatching { if (wl.isHeld) wl.release() }
        }
        wakeLock = null
    }

    private fun updateNotif() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIF_ID, buildNotif())
    }

    private fun buildNotif(): Notification {
        val s = store.state.value
        val title = when {
            !s.hasGroup -> "Idle"
            s.transmitting -> "Transmitting"
            s.transportActive && s.role == Role.Host -> "Hosting · ${s.peerCount} client${if (s.peerCount == 1) "" else "s"}"
            s.transportActive && s.role == Role.Joiner -> "Connected"
            s.talkMode == TalkMode.HandsFree -> "Listening · hands-free"
            else -> "On air · ${s.groupName.ifBlank { "Untitled" }}"
        }
        val text = when {
            !s.hasGroup -> "Tap to open · tap Stop to quit"
            s.transmitting -> "Speaking — live"
            else -> "code ${s.groupCode} · ${s.peerCount} connected"
        }

        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RadioService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, RidgeApp.CH_RADIO)
            .setContentTitle("RIDGE · $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launch)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .build()
    }

    override fun onDestroy() {
        modeJob?.cancel()
        supervisor.cancel()
        runCatching { transport.stop() }
        runCatching { vox.stop() }
        runCatching { discovery.stop() }
        exitCommunicationAudio()
        runCatching { router.release() }
        releaseWake()
        super.onDestroy()
    }

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "app.ridge.action.STOP"
    }

    private data class TransportTrigger(
        val hasGroup: Boolean,
        val role: Role,
        val hotspotActive: Boolean,
        val joinConnected: Boolean,
        val hostIp: String?,
        val joinNetwork: Network?,
    )

    private data class TransmitTrigger(
        val transportActive: Boolean,
        val hasGroup: Boolean,
        val talkMode: TalkMode,
        val transmitting: Boolean,
    )
}
