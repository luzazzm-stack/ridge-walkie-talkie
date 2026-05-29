package app.ridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import app.ridge.core.AudioOut
import app.ridge.core.AudioRouter
import app.ridge.core.HotspotHost
import app.ridge.core.HotspotState
import app.ridge.core.JoinState
import app.ridge.core.RadioService
import app.ridge.core.RidgeStore
import app.ridge.core.Role
import app.ridge.core.TalkMode
import app.ridge.core.WifiJoiner
import app.ridge.ui.screens.AudioSheet
import app.ridge.ui.screens.EmptyScreen
import app.ridge.ui.screens.GroupSetupScreen
import app.ridge.ui.screens.HostingSetupScreen
import app.ridge.ui.screens.InviteScreen
import app.ridge.ui.screens.JoinerConnectingScreen
import app.ridge.ui.screens.NameGroupScreen
import app.ridge.ui.screens.PermStatus
import app.ridge.ui.screens.SettingsScreen
import app.ridge.ui.components.ConfirmDialog
import app.ridge.ui.screens.SosScreen
import app.ridge.ui.screens.SplashScreen
import app.ridge.ui.screens.TalkScreen
import app.ridge.ui.theme.RidgeTheme
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {

    private val store get() = RidgeStore.Instance
    private lateinit var router: AudioRouter
    private lateinit var hotspot: HotspotHost
    private lateinit var joiner: WifiJoiner

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.values.all { it }
            store.setNeedsPermissions(!allGranted)
            if (allGranted) startRadioService()
        }

    private var onScanResult: ((String) -> Unit)? = null
    private val scannerLauncher = registerForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@registerForActivityResult
        onScanResult?.invoke(text)
    }

    fun launchScanner(onResult: (String) -> Unit) {
        onScanResult = onResult
        val opts = ScanOptions().apply {
            setPrompt("Point camera at RIDGE QR")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        }
        scannerLauncher.launch(opts)
    }

    /** Parses a RIDGE invite payload.
     *  Format:  ridge:CODE|NAME|SSID|PASS   (any trailing field can be missing) */
    data class Invite(
        val code: String,
        val name: String?,
        val ssid: String?,
        val pass: String?,
    )
    private fun parseInvite(payload: String): Invite {
        val trimmed = payload.trim()
        if (trimmed.startsWith("ridge:", ignoreCase = true)) {
            val parts = trimmed.substringAfter(":").split("|")
            val code = parts.getOrNull(0).orEmpty().filter { it.isDigit() }.padStart(4, '0').take(4)
            val name = parts.getOrNull(1)?.ifBlank { null }
            val ssid = parts.getOrNull(2)?.ifBlank { null }
            val pass = parts.getOrNull(3)?.ifBlank { null }
            return Invite(code, name, ssid, pass)
        }
        val digits = trimmed.filter { it.isDigit() }
        return Invite(digits.padStart(4, '0').take(4), null, null, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        router = AudioRouter(this)
        hotspot = HotspotHost(this)
        joiner = WifiJoiner(this)

        // Reflect device state into the store
        store.setBtHeadset(router.connectedBtHeadsetName(), null)
        if (router.isWiredPluggedIn()) {
            // keep wiredPluggedIn flag in sync (no direct setter, fold via state)
        }

        // Mirror hotspot + joiner flows into the store so RadioService can react
        hotspot.state.onEach { store.setHotspotState(it) }.launchIn(lifecycleScope)
        joiner.state.onEach { store.setJoinState(it) }.launchIn(lifecycleScope)

        store.setNeedsPermissions(!allPermissionsGranted())
        if (!allPermissionsGranted()) {
            // will be triggered when user taps Allow
        } else {
            startRadioService()
        }

        setContent {
            val state by store.state.collectAsState()
            RidgeTheme(night = state.night) {
                val c = RidgeTheme.colors
                Box(Modifier.fillMaxSize().background(c.bone)) {
                    AppNav()
                }
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val required = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required += Manifest.permission.BLUETOOTH_SCAN
            required += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required += Manifest.permission.NEARBY_WIFI_DEVICES
            required += Manifest.permission.POST_NOTIFICATIONS
        } else {
            required += Manifest.permission.ACCESS_FINE_LOCATION
        }
        return required.all { p ->
            ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list += Manifest.permission.BLUETOOTH_SCAN
            list += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.NEARBY_WIFI_DEVICES
            list += Manifest.permission.POST_NOTIFICATIONS
        } else {
            list += Manifest.permission.ACCESS_FINE_LOCATION
        }
        permLauncher.launch(list.toTypedArray())
    }

    private fun openAppSettings() {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(i) }
    }

    private fun startRadioService() {
        val i = Intent(this, RadioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
    }

    override fun onResume() {
        super.onResume()
        // Re-check perms after returning from Settings — user may have toggled manually.
        val nowGranted = allPermissionsGranted()
        if (nowGranted != !store.state.value.needsPermissions) {
            store.setNeedsPermissions(!nowGranted)
            if (nowGranted) startRadioService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { router.release() }
    }

    @androidx.compose.runtime.Composable
    private fun AppNav() {
        val nav = rememberNavController()
        val state by store.state.collectAsState()
        var showSheet by remember { mutableStateOf(false) }
        var showLeaveConfirm by remember { mutableStateOf(false) }

        val start = when {
            state.needsPermissions -> "splash"
            !state.hasGroup -> "empty"
            else -> "talk"
        }

        Box(Modifier.fillMaxSize()) {
        NavHost(navController = nav, startDestination = start) {
            composable("splash") {
                LaunchedEffect(state.needsPermissions) {
                    if (!state.needsPermissions) {
                        nav.navigate("empty") { popUpTo("splash") { inclusive = true } }
                    }
                }
                val perm = PermStatus(mic = !state.needsPermissions, nearby = !state.needsPermissions, notif = !state.needsPermissions)
                SplashScreen(
                    granted = perm,
                    onAllow = { requestPermissions() },
                    onOpenAppSettings = { openAppSettings() },
                )
            }
            composable("empty") {
                EmptyScreen(
                    onStart = { nav.navigate("name-group") },
                    onJoin = { nav.navigate("group-setup") }
                )
            }
            composable("name-group") {
                NameGroupScreen(
                    onStart = { name ->
                        store.startGroup(name, role = Role.Host)
                        // Don't auto-fire hotspot.start() — let user choose manual or auto on next screen.
                        nav.navigate("hosting-setup") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("hosting-setup") {
                HostingSetupScreen(
                    groupName = state.groupName,
                    state = state.hotspotState,
                    onTryAuto = { hotspot.start() },
                    onManualSave = { ssid, pass ->
                        hotspot.setManual(ssid, pass)
                        nav.navigate("talk") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onContinue = {
                        nav.navigate("talk") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onCancel = {
                        hotspot.stop()
                        store.leaveGroup()
                        nav.navigate("empty") { popUpTo("empty") { inclusive = true } }
                    },
                )
            }
            composable("group-setup") {
                GroupSetupScreen(
                    onScanQr = {
                        launchScanner { scanned ->
                            val inv = parseInvite(scanned)
                            store.startGroup(inv.name ?: "Joined group", role = Role.Joiner)
                            store.setGroupCode(inv.code)
                            if (inv.ssid != null && inv.pass != null) {
                                store.setJoinTarget(inv.ssid, inv.pass)
                                joiner.connect(inv.ssid, inv.pass)
                                nav.navigate("joiner-connecting") {
                                    popUpTo("empty") { inclusive = false }
                                }
                            } else {
                                // Code-only join — no SSID/pass yet, go to manual
                                nav.navigate("joiner-manual") {
                                    popUpTo("empty") { inclusive = false }
                                }
                            }
                        }
                    },
                    onJoinWithCode = { code ->
                        store.startGroup("Group $code", role = Role.Joiner)
                        store.setGroupCode(code)
                        // No SSID/pass in code-only path — user types them in
                        nav.navigate("joiner-manual") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("joiner-connecting") {
                JoinerConnectingScreen(
                    state = state.joinState,
                    targetSsid = state.joinSsid,
                    onRetry = { joiner.connect(state.joinSsid, state.joinPass) },
                    onConnected = {
                        nav.navigate("talk") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onCancel = {
                        joiner.disconnect()
                        store.leaveGroup()
                        nav.navigate("empty") { popUpTo("empty") { inclusive = true } }
                    },
                )
            }
            composable("joiner-manual") {
                HostingSetupScreen(
                    groupName = state.groupName,
                    state = HotspotState.ManualNeeded,
                    onRetry = { /* not applicable in joiner-manual */ },
                    onManualSave = { ssid, pass ->
                        store.setJoinTarget(ssid, pass)
                        joiner.connect(ssid, pass)
                        nav.navigate("joiner-connecting") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onContinue = { /* not used */ },
                    onCancel = {
                        store.leaveGroup()
                        nav.navigate("empty") { popUpTo("empty") { inclusive = true } }
                    },
                )
            }
            composable("talk") {
                TalkScreen(
                    state = state,
                    onConnMode = { store.setConnMode(it) },
                    onTalkMode = { store.setTalkMode(it) },
                    onPress = { down ->
                        store.setTransmitting(down)
                        // M3: AudioRecord + Opus encode + UDP send
                    },
                    onOpenAudio = { showSheet = true },
                    onSettings = { nav.navigate("settings") },
                    onInvite = { nav.navigate("invite") },
                    onLeave = { showLeaveConfirm = true },
                    onTestSos = { store.fireSos("Pemba") },
                )
                if (showSheet) {
                    AudioSheet(
                        state = state,
                        onPick = {
                            store.setAudioOut(it)
                            router.apply(it)
                        },
                        onDismiss = { showSheet = false },
                    )
                }
                if (state.sosLive) {
                    SosScreen(
                        fromName = state.sosFromName ?: "Group",
                        onRespond = { store.clearSos() },
                        onClear = { store.clearSos() },
                        onFalseAlarm = { store.clearSos() },
                    )
                }
            }
            composable("invite") {
                val ssidForQr = (state.hotspotState as? HotspotState.Active)?.ssid.orEmpty()
                val passForQr = (state.hotspotState as? HotspotState.Active)?.passphrase.orEmpty()
                InviteScreen(
                    groupName = state.groupName,
                    groupCode = state.groupCode,
                    ssid = ssidForQr,
                    pass = passForQr,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onLeaveGroup = { showLeaveConfirm = true },
                    onTheme = { store.setNight(it == 1) },
                    onQuality = { store.setVoiceQuality(it) },
                    onSos = { store.setSosArmed(it) },
                    onSaver = { store.setSaverThreshold(it) },
                    onHaptic = { store.setHaptic(it) },
                    onChime = { store.setChime(it) },
                    onBoost = { store.setBoost(it) },
                    onBtRelay = { store.setBtRelay(it) },
                )
            }
        }
        if (showLeaveConfirm) {
            ConfirmDialog(
                title = "Leave the group?",
                body = "You'll go back to the home screen. Anyone who joined with " +
                        "code ${state.groupCode} stays connected. You can rejoin " +
                        "with the same code anytime.",
                confirmLabel = "Leave",
                cancelLabel = "Stay",
                danger = true,
                onConfirm = {
                    showLeaveConfirm = false
                    hotspot.stop()
                    joiner.disconnect()
                    store.leaveGroup()
                    nav.navigate("empty") { popUpTo("empty") { inclusive = true } }
                },
                onDismiss = { showLeaveConfirm = false },
            )
        }
        }
    }
}
