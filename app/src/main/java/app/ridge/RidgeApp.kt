package app.ridge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

class RidgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ContextCompat.getSystemService(this, NotificationManager::class.java)
            if (nm != null) {
                runCatching {
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
        }
    }

    /** Catch any uncaught crash on any thread, save the stack to prefs so the
     *  next launch can show it. Then defer to the OS so behaviour is unchanged. */
    private fun installCrashLogger() {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val trace = e.stackTraceToString().take(4000)
                getSharedPreferences("ridge", Context.MODE_PRIVATE).edit()
                    .putString("last_crash", "thread=${t.name}\n$trace")
                    .commit()
            }
            prev?.uncaughtException(t, e)
        }
    }

    companion object {
        const val CH_RADIO = "radio"
        const val CH_SOS = "sos"
    }
}
