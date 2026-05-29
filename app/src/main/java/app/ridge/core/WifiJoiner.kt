package app.ridge.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class JoinState {
    object Disconnected : JoinState()
    data class Connecting(val ssid: String) : JoinState()
    data class Connected(val ssid: String, val network: Network, val gatewayIp: String) : JoinState()
    data class Failed(val reason: String) : JoinState()
}

/**
 * Connects this device to a specific Wi-Fi network (RIDGE host's hotspot)
 * without binding the whole device — only RIDGE's traffic uses the link, so
 * the user's other apps keep using cellular.
 *
 * Android 10+ → WifiNetworkSpecifier (the user gets a system prompt).
 * Android < 10 → falls back to ManualPlease state; legacy WifiConfiguration is
 *                deprecated and unreliable. Most users on those versions are
 *                better off connecting manually from Wi-Fi settings.
 */
class WifiJoiner(private val ctx: Context) {

    private val _state = MutableStateFlow<JoinState>(JoinState.Disconnected)
    val state: StateFlow<JoinState> = _state.asStateFlow()

    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null

    @SuppressLint("MissingPermission")
    fun connect(ssid: String, passphrase: String, timeoutMs: Long = 25_000L) {
        disconnect()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _state.value = JoinState.Failed(
                "Auto-connect needs Android 10+. Go to Wi-Fi settings and join \"$ssid\" with password $passphrase."
            )
            return
        }
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            _state.value = JoinState.Failed("No connectivity manager")
            return
        }
        _state.value = JoinState.Connecting(ssid)

        val spec = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(passphrase)
            .build()

        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(spec)
            .build()

        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val gw = gatewayOf(network) ?: "192.168.43.1"
                _state.value = JoinState.Connected(ssid, network, gw)
            }
            override fun onUnavailable() {
                _state.value = JoinState.Failed("Could not connect to \"$ssid\"")
            }
            override fun onLost(network: Network) {
                _state.value = JoinState.Disconnected
            }
        }
        connectivityCallback = cb
        try {
            cm.requestNetwork(req, cb, timeoutMs.toInt())
        } catch (t: Throwable) {
            _state.value = JoinState.Failed(t.message ?: "request failed")
        }
    }

    fun disconnect() {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityCallback?.let { runCatching { cm?.unregisterNetworkCallback(it) } }
        connectivityCallback = null
        _state.value = JoinState.Disconnected
    }

    /** Returns the connected Network (or null), so a VoiceTransport socket can be bound to it. */
    fun activeNetwork(): Network? = (_state.value as? JoinState.Connected)?.network

    private fun gatewayOf(network: Network): String? {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val link = cm.getLinkProperties(network) ?: return null
        return link.dhcpServerAddress?.hostAddress
            ?: link.routes.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
    }
}
