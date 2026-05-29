package app.ridge.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class JoinState {
    object Disconnected : JoinState()
    data class Connecting(val ssid: String) : JoinState()
    data class Connected(val ssid: String, val network: Network?, val gatewayIp: String) : JoinState()
    data class Failed(val reason: String) : JoinState()
}

/**
 * Detects whether the user has manually connected to the host's hotspot via
 * Android Wi-Fi settings. We deliberately do NOT auto-connect via
 * WifiNetworkSpecifier — it's flaky on OEM ROMs (Realme, Xiaomi) and the
 * normal "join Wi-Fi from settings" path is universally reliable.
 *
 * Usage: user opens Android Wi-Fi settings, joins the host's hotspot, returns
 * to RIDGE, taps "Try connecting", we read the current Wi-Fi network's
 * gateway IP and treat that as the host.
 */
class WifiJoiner(private val ctx: Context) {

    private val _state = MutableStateFlow<JoinState>(JoinState.Disconnected)
    val state: StateFlow<JoinState> = _state.asStateFlow()

    fun expectSsid(ssid: String) {
        _state.value = JoinState.Connecting(ssid.ifBlank { "host" })
    }

    /** Look at the currently joined Wi-Fi network and treat its gateway as the host.
     *  Returns true if a Wi-Fi connection was found. */
    fun detectCurrentWifi(expectedSsid: String? = null): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (cm == null || wm == null) {
            _state.value = JoinState.Failed("Wi-Fi service not available")
            return false
        }

        // Find a Wi-Fi network in the active networks
        val wifiNetwork = findWifiNetwork(cm)
        val gateway = gatewayString(wm, cm, wifiNetwork)

        if (gateway == null) {
            _state.value = JoinState.Failed(
                "No Wi-Fi connection detected. Join the host's hotspot in Wi-Fi settings, then come back."
            )
            return false
        }

        val ssidGuess = expectedSsid?.ifBlank { null } ?: "connected Wi-Fi"
        _state.value = JoinState.Connected(ssidGuess, wifiNetwork, gateway)
        return true
    }

    fun disconnect() {
        _state.value = JoinState.Disconnected
    }

    fun activeNetwork(): Network? = (_state.value as? JoinState.Connected)?.network

    private fun findWifiNetwork(cm: ConnectivityManager): Network? {
        // Try activeNetwork first
        val active = cm.activeNetwork
        if (active != null && isWifi(cm, active)) return active
        // Walk all networks (Wi-Fi hotspots have no internet → not "active")
        return runCatching {
            cm.allNetworks.firstOrNull { isWifi(cm, it) }
        }.getOrNull()
    }

    private fun isWifi(cm: ConnectivityManager, n: Network): Boolean {
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun gatewayString(wm: WifiManager, cm: ConnectivityManager, network: Network?): String? {
        // 1) ConnectivityManager LinkProperties (more reliable on modern Android)
        if (network != null) {
            runCatching {
                val link = cm.getLinkProperties(network)
                val viaRoutes = link?.routes?.firstOrNull { it.isDefaultRoute }
                    ?.gateway?.hostAddress
                if (!viaRoutes.isNullOrBlank()) return viaRoutes
            }
        }
        // 2) Legacy DhcpInfo (works on older Android + many OEMs)
        @Suppress("DEPRECATION")
        val gw = runCatching { wm.dhcpInfo?.gateway ?: 0 }.getOrNull() ?: 0
        if (gw != 0) return intToIpv4(gw)
        return null
    }

    private fun intToIpv4(ip: Int): String {
        return "${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"
    }
}
