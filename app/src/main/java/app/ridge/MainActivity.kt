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
            // Some OEMs return an empty map if they suppress the dialog — don't
            // treat that as "all granted". Mic is the only hard requirement.
            if (result.isEmpty()) { store.setNeedsPermissions(!micGranted()); return@registerForActivityResult }
            val ok = micGranted()
            store.setNeedsPermissions(!ok)
            if (ok) startRadioService()
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

        // Load saved display name (default to the phone model on first run)
        val prefs = getSharedPreferences("ridge", MODE_PRIVATE)
        val savedName = prefs.getString("my_name", null)
            ?.takeIf { it.isNotBlank() }
            ?: (Build.MODEL?.takeIf { it.isNotBlank() } ?: "Me")
        store.setMyName(savedName)

        // Mic is the only hard requirement; everything else is optional and
        // must never trap the user on the splash screen.
        store.setNeedsPermissions(!micGranted())
        if (micGranted()) startRadioService()

        val savedCrash = runCatching {
            getSharedPreferences("ridge", MODE_PRIVATE).getString("last_crash", null)
        }.getOrNull()

        setContent {
            val state by store.state.collectAsState()
            var crash by remember { mutableStateOf(savedCrash) }
            RidgeTheme(night = state.night) {
                val c = RidgeTheme.colors
                Box(Modifier.fillMaxSize().background(c.bone)) {
                    AppNav()
                    val cr = crash
                    if (!cr.isNullOrBlank()) {
                        app.ridge.ui.components.CrashDialog(
                            trace = cr,
                            onCopy = {},
                            onDismiss = {
                                crash = null
                                runCatching {
                                    getSharedPreferences("ridge", MODE_PRIVATE).edit()
                                        .remove("last_crash").apply()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    /** The only permission that actually blocks the app from working. */
    private fun micGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

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

    private fun openWifiSettings() {
        val i = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(i) }
    }

    private fun startRadioService() {
        val i = Intent(this, RadioService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
        } catch (e: Throwable) {
            // Android 12+ can reject a FGS start during the transient background
            // window right after a permission dialog. Retry on the next frame.
            window.decorView.postDelayed({
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
                    else startService(i)
                }
            }, 300)
        }
    }

    private fun saveMyName(name: String) {
        val clean = name.trim().take(24).ifBlank { "Me" }
        store.setMyName(clean)
        runCatching {
            getSharedPreferences("ridge", MODE_PRIVATE).edit().putString("my_name", clean).apply()
        }
    }

    private fun stopRadioAndQuit() {
        runCatching { hotspot.stop() }
        runCatching { joiner.disconnect() }
        store.leaveGroup()
        val i = Intent(this, RadioService::class.java).apply { action = RadioService.ACTION_STOP }
        runCatching { startService(i) }
        finishAndRemoveTask()
    }

    override fun onResume() {
        super.onResume()
        // Re-check mic after returning from Settings — user may have toggled it.
        val ok = micGranted()
        if (ok == store.state.value.needsPermissions) {  // state disagrees with reality
            store.setNeedsPermissions(!ok)
            if (ok) startRadioService()
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

        // Compute the start destination ONCE — recomputing it every recomposition
        // corrupts the NavHost back-stack (popUpTo("empty") on a stack that never
        // had "empty"). Navigation between states is handled explicitly elsewhere.
        val start = remember {
            when {
                store.state.value.needsPermissions -> "splash"
                !store.state.value.hasGroup -> "empty"
                else -> "talk"
            }
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
                            // Pre-fill what we know; user joins manually in Wi-Fi settings.
                            store.setJoinTarget(inv.ssid ?: "", inv.pass ?: "")
                            joiner.expectSsid(inv.ssid ?: "")
                            nav.navigate("joiner-connecting") {
                                popUpTo("empty") { inclusive = false }
                            }
                        }
                    },
                    onJoinWithCode = { code ->
                        store.startGroup("Group $code", role = Role.Joiner)
                        store.setGroupCode(code)
                        store.setJoinTarget("", "")
                        joiner.expectSsid("")
                        nav.navigate("joiner-connecting") {
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
                    targetPass = state.joinPass,
                    onOpenWifiSettings = { openWifiSettings() },
                    onTryConnect = {
                        joiner.detectCurrentWifi(state.joinSsid)
                    },
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
                    onStopApp = { stopRadioAndQuit() },
                    onSaveName = { saveMyName(it) },
                    onTheme = { store.setNight(it == 1) },
                    onSos = { store.setSosArmed(it) },
                    onHaptic = { store.setHaptic(it) },
                    onChime = { store.setChime(it) },
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
