package app.ridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import app.ridge.core.AudioOut
import app.ridge.core.AudioRouter
import app.ridge.core.RadioService
import app.ridge.core.RidgeStore
import app.ridge.core.TalkMode
import app.ridge.ui.screens.AudioSheet
import app.ridge.ui.screens.EmptyScreen
import app.ridge.ui.screens.GroupSetupScreen
import app.ridge.ui.screens.NameGroupScreen
import app.ridge.ui.screens.PermStatus
import app.ridge.ui.screens.SettingsScreen
import app.ridge.ui.screens.SosScreen
import app.ridge.ui.screens.SplashScreen
import app.ridge.ui.screens.TalkScreen
import app.ridge.ui.theme.RidgeTheme

class MainActivity : ComponentActivity() {

    private val store get() = RidgeStore.Instance
    private lateinit var router: AudioRouter

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.values.all { it }
            store.setNeedsPermissions(!allGranted)
            if (allGranted) startRadioService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        router = AudioRouter(this)

        // Reflect device state into the store
        store.setBtHeadset(router.connectedBtHeadsetName(), null)
        if (router.isWiredPluggedIn()) {
            // keep wiredPluggedIn flag in sync (no direct setter, fold via state)
        }

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

    private fun startRadioService() {
        val i = Intent(this, RadioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
        else startService(i)
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

        val start = when {
            state.needsPermissions -> "splash"
            !state.hasGroup -> "empty"
            else -> "talk"
        }

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
                    onAllow = { requestPermissions() }
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
                        store.startGroup(name)
                        nav.navigate("talk") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("group-setup") {
                GroupSetupScreen(
                    code = state.groupCode,
                    onJoin = {
                        store.startGroup("Joined group")
                        nav.navigate("talk") {
                            popUpTo("empty") { inclusive = false }
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("talk") {
                TalkScreen(
                    state = state,
                    onConnMode = { store.setConnMode(it) },
                    onTalkMode = { store.setTalkMode(it) },
                    onPress = { down ->
                        store.setTransmitting(down)
                        // M2: AudioRecord + Opus encode + UDP send
                    },
                    onOpenAudio = { showSheet = true },
                    onSettings = { nav.navigate("settings") },
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
            composable("settings") {
                SettingsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onLeaveGroup = {
                        store.leaveGroup()
                        nav.navigate("empty") { popUpTo("empty") { inclusive = true } }
                    },
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
    }
}
