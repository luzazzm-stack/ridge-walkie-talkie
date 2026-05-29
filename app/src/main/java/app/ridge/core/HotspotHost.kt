package app.ridge.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class HotspotState {
    object Idle : HotspotState()
    object Starting : HotspotState()
    data class Active(val ssid: String, val passphrase: String) : HotspotState()
    data class Failed(val reason: String) : HotspotState()
    /** Local-only hotspot unavailable; user must enable mobile hotspot manually
     *  and type the SSID + passphrase into the app. */
    object ManualNeeded : HotspotState()
}

/**
 * Wraps WifiManager.startLocalOnlyHotspot(). Always falls back to ManualNeeded
 * so the UI can guide the user to enable their mobile hotspot themselves
 * when local-only isn't supported (rare) or denied.
 */
class HotspotHost(private val ctx: Context) {

    private val _state = MutableStateFlow<HotspotState>(HotspotState.Idle)
    val state: StateFlow<HotspotState> = _state.asStateFlow()

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun start() {
        if (_state.value is HotspotState.Active || _state.value is HotspotState.Starting) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            _state.value = HotspotState.ManualNeeded
            return
        }
        _state.value = HotspotState.Starting
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifi == null) {
            _state.value = HotspotState.ManualNeeded
            return
        }
        try {
            wifi.startLocalOnlyHotspot(
                object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(r: WifiManager.LocalOnlyHotspotReservation) {
                        reservation = r
                        _state.value = HotspotState.Active(
                            ssid = ssidOf(r) ?: "RIDGE",
                            passphrase = passOf(r) ?: ""
                        )
                    }
                    override fun onStopped() {
                        reservation = null
                        if (_state.value is HotspotState.Active) _state.value = HotspotState.Idle
                    }
                    override fun onFailed(reason: Int) {
                        reservation = null
                        _state.value = HotspotState.ManualNeeded
                    }
                },
                handler
            )
        } catch (t: Throwable) {
            _state.value = HotspotState.ManualNeeded
        }
    }

    /** Caller can switch into manual mode at any time with their own SSID + pass. */
    fun setManual(ssid: String, passphrase: String) {
        stop()
        _state.value = HotspotState.Active(ssid = ssid, passphrase = passphrase)
    }

    fun stop() {
        runCatching { reservation?.close() }
        reservation = null
        _state.value = HotspotState.Idle
    }

    private fun ssidOf(r: WifiManager.LocalOnlyHotspotReservation): String? {
        // softApConfiguration (API 30+) preferred; wifiConfiguration is older
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cfg = runCatching { r.softApConfiguration }.getOrNull() ?: return legacySsid(r)
            return runCatching { cfg.wifiSsid?.toString()?.trim('"') }.getOrNull() ?: legacySsid(r)
        }
        return legacySsid(r)
    }

    private fun legacySsid(r: WifiManager.LocalOnlyHotspotReservation): String? {
        @Suppress("DEPRECATION")
        return runCatching { r.wifiConfiguration?.SSID?.trim('"') }.getOrNull()
    }

    private fun passOf(r: WifiManager.LocalOnlyHotspotReservation): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val cfg = runCatching { r.softApConfiguration }.getOrNull() ?: return legacyPass(r)
            return runCatching { cfg.passphrase }.getOrNull() ?: legacyPass(r)
        }
        return legacyPass(r)
    }

    private fun legacyPass(r: WifiManager.LocalOnlyHotspotReservation): String? {
        @Suppress("DEPRECATION")
        return runCatching { r.wifiConfiguration?.preSharedKey?.trim('"') }.getOrNull()
    }
}
