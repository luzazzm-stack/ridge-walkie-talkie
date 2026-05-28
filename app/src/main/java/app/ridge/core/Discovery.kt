package app.ridge.core

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NearbyPeer(
    val id: String,
    val name: String,
    val rssi: Int = -60,
    val viaBt: Boolean = false,
)

/**
 * Lightweight discovery wrapper. Best-effort; failures are silent so the
 * service / UI keeps running even if a transport is denied or unavailable.
 *
 * Voice transport (UDP/Opus) is M2 — this only finds peers.
 */
class Discovery(private val ctx: Context) {

    private val _peers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    val peers: StateFlow<List<NearbyPeer>> = _peers.asStateFlow()

    private val bm by lazy { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    private val wp by lazy { ctx.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager }
    private var wpChannel: WifiP2pManager.Channel? = null
    private val wpReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
                val mgr = wp ?: return
                val ch = wpChannel ?: return
                if (!hasPerm(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES
                             else Manifest.permission.ACCESS_FINE_LOCATION)) return
                runCatching {
                    mgr.requestPeers(ch) { list ->
                        val mapped = list.deviceList.map(::toNearby)
                        merge(mapped, viaBt = false)
                    }
                }
            }
        }
    }
    private var wpReceiverRegistered = false

    private val btCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val addr = result.device.address ?: return
            val name = runCatching { result.device.name }.getOrNull() ?: "Device ${addr.takeLast(5)}"
            merge(listOf(NearbyPeer(id = "bt:$addr", name = name, rssi = result.rssi, viaBt = true)), viaBt = true)
        }
        override fun onScanFailed(errorCode: Int) {}
    }
    private var btScanning = false

    fun start() {
        startBle()
        startWifiP2p()
    }

    fun stop() {
        stopBle()
        stopWifiP2p()
    }

    @SuppressLint("MissingPermission")
    private fun startBle() {
        if (!hasPerm(Manifest.permission.BLUETOOTH_SCAN)) return
        val scanner = bm?.adapter?.bluetoothLeScanner ?: return
        if (btScanning) return
        runCatching {
            scanner.startScan(
                null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(),
                btCallback
            )
            btScanning = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBle() {
        if (!btScanning) return
        runCatching { bm?.adapter?.bluetoothLeScanner?.stopScan(btCallback) }
        btScanning = false
    }

    @SuppressLint("MissingPermission")
    private fun startWifiP2p() {
        val mgr = wp ?: return
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES
                   else Manifest.permission.ACCESS_FINE_LOCATION
        if (!hasPerm(perm)) return
        if (wpChannel == null) {
            wpChannel = mgr.initialize(ctx, Looper.getMainLooper(), null)
        }
        if (!wpReceiverRegistered) {
            val f = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(wpReceiver, f, Context.RECEIVER_NOT_EXPORTED)
            } else {
                ctx.registerReceiver(wpReceiver, f)
            }
            wpReceiverRegistered = true
        }
        runCatching {
            mgr.discoverPeers(wpChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}
                override fun onFailure(reason: Int) {}
            })
        }
    }

    private fun stopWifiP2p() {
        val mgr = wp ?: return
        val ch = wpChannel
        if (ch != null) runCatching {
            mgr.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {}
                override fun onFailure(reason: Int) {}
            })
        }
        if (wpReceiverRegistered) {
            runCatching { ctx.unregisterReceiver(wpReceiver) }
            wpReceiverRegistered = false
        }
    }

    private fun hasPerm(p: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    private fun toNearby(d: WifiP2pDevice): NearbyPeer =
        NearbyPeer(id = "wp:${d.deviceAddress}", name = d.deviceName ?: "Wi-Fi peer", rssi = -55, viaBt = false)

    private fun merge(found: List<NearbyPeer>, viaBt: Boolean) {
        if (found.isEmpty()) return
        _peers.update { old ->
            val map = old.associateBy { it.id }.toMutableMap()
            found.forEach { p -> map[p.id] = p }
            map.values.toList()
        }
    }
}
