package app.ridge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.ContextCompat

class RidgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)!!
            nm.createNotificationChannel(
                NotificationChannel(CH_RADIO, "Radio", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Persistent while a group is active"
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_SOS, "SOS", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Emergency alerts from your group"
                    enableVibration(true)
                    setBypassDnd(true)
                }
            )
        }
    }
    companion object {
        const val CH_RADIO = "radio"
        const val CH_SOS = "sos"
    }
}
