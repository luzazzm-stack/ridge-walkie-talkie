package app.ridge.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import app.ridge.MainActivity
import app.ridge.R
import app.ridge.RidgeApp

class RadioService : LifecycleService() {

    private lateinit var router: AudioRouter
    private lateinit var discovery: Discovery

    override fun onCreate() {
        super.onCreate()
        router = AudioRouter(this)
        discovery = Discovery(this)
        startForeground(NOTIF_ID, buildNotif("Ready", "Open RIDGE to start"))
        discovery.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                buildNotif("Ready", "Open RIDGE to start"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { discovery.stop() }
        runCatching { router.release() }
        super.onDestroy()
    }

    private fun buildNotif(title: String, text: String): Notification {
        val launch = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, RidgeApp.CH_RADIO)
            .setContentTitle("RIDGE · $title")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(launch)
            .build()
    }

    companion object { const val NOTIF_ID = 1001 }
}
