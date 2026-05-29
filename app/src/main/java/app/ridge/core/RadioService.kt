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
    private var lastNetwork: Network? = null

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
            onError = { /* surface to UI via store later; ignore for now */ },
        )
        startForeground(NOTIF_ID, buildNotif())
        discovery.start()
        observeLifecycle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                buildNotif(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        }
        return START_STICKY
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
                    )
                }
                .distinctUntilChanged()
                .onEach { trig ->
                    val wantTransport = trig.hasGroup && (
                            (trig.role == Role.Host && trig.hotspotActive) ||
                                    (trig.role == Role.Joiner && trig.joinConnected)
                            )
                    if (wantTransport && !transportRunning) {
                        when (trig.role) {
                            Role.Host -> transport.startHost(network = null)
                            Role.Joiner -> transport.startClient(trig.hostIp ?: "192.168.43.1", lastNetwork)
                            else -> Unit
                        }
                        transportRunning = true
                        store.setTransportActive(true)
                        vox.stop() // hand mic over to transport
                    } else if (!wantTransport && transportRunning) {
                        transport.stop()
                        transportRunning = false
                        store.setTransportActive(false)
                    }
                    updateNotif()
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
                }
                .launchIn(this)
        }
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
            !s.hasGroup -> "Open RIDGE to start"
            s.transmitting -> "Speaking — live"
            else -> "code ${s.groupCode} · ${s.peerCount} connected"
        }

        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
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
            .build()
    }

    override fun onDestroy() {
        modeJob?.cancel()
        supervisor.cancel()
        runCatching { transport.stop() }
        runCatching { vox.stop() }
        runCatching { discovery.stop() }
        runCatching { router.release() }
        releaseWake()
        super.onDestroy()
    }

    fun setActiveNetwork(network: Network?) { lastNetwork = network }

    companion object { const val NOTIF_ID = 1001 }

    private data class TransportTrigger(
        val hasGroup: Boolean,
        val role: Role,
        val hotspotActive: Boolean,
        val joinConnected: Boolean,
        val hostIp: String?,
    )

    private data class TransmitTrigger(
        val transportActive: Boolean,
        val hasGroup: Boolean,
        val talkMode: TalkMode,
        val transmitting: Boolean,
    )
}
