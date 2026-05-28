package app.ridge.core

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RadioService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    private lateinit var router: AudioRouter
    private lateinit var discovery: Discovery
    private lateinit var vox: VoxRecorder
    private val store get() = RidgeStore.Instance

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + supervisor)
    private var modeJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null

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
        startForeground(NOTIF_ID, buildNotif())
        discovery.start()
        observeMode()
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

    private fun observeMode() {
        modeJob?.cancel()
        modeJob = scope.launch {
            store.state
                .map { Triple(it.hasGroup, it.talkMode, it.transmitting) }
                .distinctUntilChanged()
                .collect { (hasGroup, mode, transmitting) ->
                    val wantVox = hasGroup && mode == TalkMode.HandsFree
                    if (wantVox) {
                        val started = vox.start()
                        if (started) acquireWake() else releaseWake()
                    } else {
                        vox.stop()
                        // hold mode: wake only while user is pressing PTT
                        if (transmitting) acquireWake() else releaseWake()
                    }
                    updateNotif()
                }
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
            s.talkMode == TalkMode.HandsFree -> "Listening · hands-free"
            else -> "On air · ${s.groupName.ifBlank { "Untitled" }}"
        }
        val text = when {
            !s.hasGroup -> "Open RIDGE to start"
            s.transmitting -> "Speaking — live on group"
            else -> "code ${s.groupCode} · ${s.members.size} member${if (s.members.size == 1) "" else "s"}"
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
        runCatching { vox.stop() }
        runCatching { discovery.stop() }
        runCatching { router.release() }
        releaseWake()
        super.onDestroy()
    }

    companion object { const val NOTIF_ID = 1001 }
}
