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
    data class Failed(val reason: String, val diagnostics: List<String> = emptyList()) : JoinState()
}

/**
 * Detects whether the user has manually connected to the host's hotspot via
 * Android Wi-Fi settings.
 *
 * No auto-connect (WifiNetworkSpecifier is flaky on OEM ROMs and Android < 10).
 * The user joins the host's Wi-Fi the normal way; we read the current
 * connection's gateway IP and treat that as the host's UDP server.
 *
 * Three detection paths, tried in order:
 *   1. ConnectivityManager → LinkProperties → default route gateway
 *   2. WifiManager → DhcpInfo.gateway
 *   3. WifiManager → connectionInfo.ipAddress → assume gateway is .1 of subnet
 */
class WifiJoiner(private val ctx: Context) {

    private val _state = MutableStateFlow<JoinState>(JoinState.Disconnected)
    val state: StateFlow<JoinState> = _state.asStateFlow()

    /** Just record what SSID to display — does NOT flip to Connecting. */
    fun expectSsid(ssid: String) {
        // no-op on state; UI reads target SSID from store.joinSsid
    }

    /** Read the currently joined Wi-Fi network and treat its gateway as the host. */
    fun detectCurrentWifi(expectedSsid: String? = null): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val diag = mutableListOf<String>()

        if (cm == null || wm == null) {
            _state.value = JoinState.Failed("Wi-Fi service unavailable", diag + "no system services")
            return false
        }

        @Suppress("DEPRECATION")
        val wifiEnabled = wm.isWifiEnabled
        diag += "wifi enabled: $wifiEnabled"
        if (!wifiEnabled) {
            _state.value = JoinState.Failed("Wi-Fi is off. Turn it on.", diag)
            return false
        }

        // ── Path 1: ConnectivityManager
        val wifiNetwork = findWifiNetwork(cm, diag)
        var gateway = if (wifiNetwork != null) viaLinkProperties(cm, wifiNetwork, diag) else null

        // ── Path 2: DhcpInfo
        if (gateway == null) gateway = viaDhcpInfo(wm, diag)

        // ── Path 3: infer from my IP
        if (gateway == null) gateway = viaInferenceFromMyIp(wm, diag)

        if (gateway == null) {
            _state.value = JoinState.Failed(
                "No Wi-Fi connection detected. Join the host's hotspot in Wi-Fi settings first.",
                diag,
            )
            return false
        }

        @Suppress("DEPRECATION")
        val ssidGuess = (expectedSsid?.ifBlank { null })
            ?: runCatching { wm.connectionInfo?.ssid?.trim('"') }.getOrNull()?.ifBlank { null }
            ?: "connected Wi-Fi"

        diag += "gateway: $gateway"
        _state.value = JoinState.Connected(ssidGuess, wifiNetwork, gateway)
        return true
    }

    fun disconnect() {
        _state.value = JoinState.Disconnected
    }

    fun activeNetwork(): Network? = (_state.value as? JoinState.Connected)?.network

    // ─────────────── detection helpers ───────────────

    private fun findWifiNetwork(cm: ConnectivityManager, diag: MutableList<String>): Network? {
        val active = cm.activeNetwork
        if (active != null && isWifi(cm, active)) { diag += "activeNetwork is Wi-Fi"; return active }
        diag += "activeNetwork: ${if (active == null) "null" else "not Wi-Fi"}"
        val all = runCatching { cm.allNetworks.toList() }.getOrNull().orEmpty()
        diag += "allNetworks: ${all.size}"
        return all.firstOrNull { isWifi(cm, it) }
    }

    private fun isWifi(cm: ConnectivityManager, n: Network): Boolean {
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun viaLinkProperties(cm: ConnectivityManager, network: Network, diag: MutableList<String>): String? {
        return runCatching {
            val link = cm.getLinkProperties(network) ?: return@runCatching null
            val gw = link.routes.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
            diag += "LinkProperties gateway: $gw"
            gw?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun viaDhcpInfo(wm: WifiManager, diag: MutableList<String>): String? {
        val gw = runCatching { wm.dhcpInfo?.gateway ?: 0 }.getOrNull() ?: 0
        diag += "DhcpInfo gateway: ${if (gw == 0) "0/unavailable" else intToIpv4(gw)}"
        return if (gw != 0) intToIpv4(gw) else null
    }

    @Suppress("DEPRECATION")
    private fun viaInferenceFromMyIp(wm: WifiManager, diag: MutableList<String>): String? {
        val info = runCatching { wm.connectionInfo }.getOrNull()
        val myIp = info?.ipAddress ?: 0
        if (myIp == 0) {
            diag += "connectionInfo.ipAddress: 0/none"
            return null
        }
        val myIpStr = intToIpv4(myIp)
        // replace last octet with .1
        val masked = (myIp and 0x00FFFFFF) or 0x01000000
        val inferred = intToIpv4(masked)
        diag += "my IP: $myIpStr → inferred gateway: $inferred"
        return inferred
    }

    private fun intToIpv4(ip: Int): String =
        "${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"
}
