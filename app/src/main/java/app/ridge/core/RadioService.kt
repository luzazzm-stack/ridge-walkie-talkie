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
    private lateinit var vox: VoxRecorder
    private lateinit var transport: VoiceTransport
    private val store get() = RidgeStore.Instance

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisor)
    private var modeJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private var transportRunning = false
    private var audioModeActive = false
    @Volatile private var stopping = false

    /** One true teardown path — used by the notification Stop, in-app Stop, and
     *  when the app is swiped from recents. Sets `stopping` first so async
     *  callbacks can't re-post the notification after we've removed it. */
    private fun fullStop() {
        stopping = true
        modeJob?.cancel()
        runCatching { transport.stop() }
        runCatching { vox.stop() }
        transportRunning = false   // MUST reset — else a restart on this same instance
                                   // sees stale true and never re-starts the transport.
        exitCommunicationAudio()
        releaseWake()
        store.leaveGroup()
        runCatching {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(NOTIF_ID)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        router = AudioRouter(this)
        vox = VoxRecorder(
            ctx = this,
            onLevel = { lvl -> if (!stopping) store.setVoxLevel((lvl * 100).toInt()) },
            onActiveChanged = { active ->
                if (!stopping) { store.setTransmitting(active); updateNotif() }
            },
        )
        transport = VoiceTransport(
            ctx = this,
            onLevel = { lvl -> if (!stopping) store.setVoxLevel((lvl * 100).toInt()) },
            onActiveChanged = { active ->
                if (!stopping) {
                    store.setTransmitting(active)
                    if (active && store.state.value.haptic) buzz()
                    updateNotif()
                }
            },
            onPeers = { names -> if (!stopping) { store.setPeers(names); updateNotif() } },
            onError = { e -> if (!stopping) store.setLastError(e) },
            onStats = { tx, rx -> if (!stopping) store.setStats(tx, rx) },
            onIncomingStart = { if (!stopping && store.state.value.chime) chime() },
            onDiag = { myIp, target -> if (!stopping) store.setDiag(myIp, target) },
        )
        startForegroundTyped()
        observeLifecycle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            fullStop()
            return START_NOT_STICKY
        }
        stopping = false                 // normal (re)start clears prior stopping state
        ensureObserving()                // relaunch collectors if a prior fullStop cancelled modeJob
        startForegroundTyped()
        return START_STICKY
    }

    /** observeLifecycle() launches the store collectors into modeJob. fullStop()
     *  cancels modeJob; if the SAME service instance is reused for a later start
     *  (stopSelf is deferred), the collectors would otherwise stay dead and the
     *  radio would never engage again. Relaunch when missing. */
    private fun ensureObserving() {
        if (modeJob?.isActive != true) observeLifecycle()
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
                    if (stopping) return@onEach   // don't resurrect transport mid-teardown
                    try {
                        val wantTransport = trig.hasGroup && (
                                (trig.role == Role.Host && trig.hotspotActive) ||
                                        (trig.role == Role.Joiner && trig.joinConnected)
                                )
                        if (wantTransport && !transportRunning) {
                            // hand mic over from preview VOX, give it a moment to release
                            vox.stop()
                            kotlinx.coroutines.delay(150)
                            enterCommunicationAudio()
                            transport.setMyName(store.state.value.myName.ifBlank { deviceName() })
                            val started = when (trig.role) {
                                Role.Host -> transport.startHost(network = null)
                                Role.Joiner -> transport.startClient(
                                    trig.hostIp ?: "192.168.43.1",
                                    trig.joinNetwork,
                                )
                                else -> false
                            }
                            // Only claim "connected" if the socket actually opened.
                            transportRunning = started
                            store.setTransportActive(started)
                            if (!started) exitCommunicationAudio()
                        } else if (!wantTransport && transportRunning) {
                            transport.stop()
                            transportRunning = false
                            store.setTransportActive(false)
                            exitCommunicationAudio()
                        }
                        updateNotif()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e   // let cancellation unwind cleanly (don't record as an error)
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
                    if (stopping) return@onEach
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
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        store.setLastError("transmit: ${e.message}")
                    }
                }
                .launchIn(this)
        }
    }

    private fun enterCommunicationAudio() {
        if (audioModeActive) return
        // Media-stream playback works in MODE_NORMAL; don't force telephony mode.
        // Just make sure the media volume isn't sitting at zero.
        runCatching {
            val am = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
            val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            if (cur < max / 2) {
                am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (max * 0.8).toInt(), 0)
            }
        }
        audioModeActive = true
    }

    private fun exitCommunicationAudio() {
        if (!audioModeActive) return
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
        if (stopping) return  // don't resurrect the notification mid-teardown
        runCatching {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NOTIF_ID, buildNotif())
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App swiped away from recents → stop the radio + notification.
        fullStop()
        super.onTaskRemoved(rootIntent)
    }

    private fun deviceName(): String =
        android.os.Build.MODEL?.takeIf { it.isNotBlank() } ?: "Member"

    private fun buzz() {
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(40)
            }
        }
    }

    private fun chime() {
        runCatching {
            val tg = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_MUSIC, 80
            )
            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
            // release shortly after the tone finishes
            android.os.Handler(mainLooper).postDelayed({ runCatching { tg.release() } }, 400)
        }
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
