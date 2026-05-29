package app.ridge.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.HotspotState
import app.ridge.ui.components.HazardTape
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun HostingSetupScreen(
    groupName: String,
    state: HotspotState,
    onTryAuto: () -> Unit,
    onManualSave: (ssid: String, pass: String) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = RidgeTheme.colors
    var manualSsid by remember { mutableStateOf("") }
    var manualPass by remember { mutableStateOf("") }

    // When auto fires onStarted, prefill the manual fields so user can review.
    LaunchedEffect(state) {
        if (state is HotspotState.Active) {
            if (manualSsid.isBlank()) manualSsid = state.ssid
            if (manualPass.isBlank()) manualPass = state.passphrase
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bone)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper)
                    .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(8.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Column {
                Text(
                    "Set up hotspot",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp,
                    color = c.ink,
                )
                Text(
                    "for · ${groupName.ifBlank { "Untitled" }}",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 11.sp,
                    color = c.muted,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ────────── 3-STEP INSTRUCTIONS ──────────
        RidgeCard(Modifier.fillMaxWidth()) {
            Column {
                HazardTape()
                Column(Modifier.padding(16.dp)) {
                    StepRow(num = "1", title = "Turn on your phone's hotspot",
                            body = "Open Quick Settings (swipe down twice) and tap Mobile Hotspot. Or: Settings → Connections → Mobile Hotspot and tethering.")
                    Spacer(Modifier.height(10.dp))
                    StepRow(num = "2", title = "Read its name and password",
                            body = "Look at the hotspot screen in Settings. You'll see a Wi-Fi name (SSID) and a password.")
                    Spacer(Modifier.height(10.dp))
                    StepRow(num = "3", title = "Type them below",
                            body = "RIDGE puts them into your QR so friends scan-and-join — no manual typing on their side.")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ────────── MANUAL INPUT ──────────
        RidgeLabel("Hotspot name (SSID)")
        Spacer(Modifier.height(6.dp))
        RidgeCard(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = manualSsid,
                onValueChange = { if (it.length <= 32) manualSsid = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = c.ink,
                ),
                cursorBrush = SolidColor(c.hivis),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                decorationBox = { inner ->
                    if (manualSsid.isEmpty()) Text(
                        "e.g. AndroidAP1234",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = c.muted.copy(alpha = 0.5f),
                    )
                    inner()
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        RidgeLabel("Password")
        Spacer(Modifier.height(6.dp))
        RidgeCard(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = manualPass,
                onValueChange = { if (it.length <= 64) manualPass = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = c.ink,
                ),
                cursorBrush = SolidColor(c.hivis),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                decorationBox = { inner ->
                    if (manualPass.isEmpty()) Text(
                        "hotspot password",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = c.muted.copy(alpha = 0.5f),
                    )
                    inner()
                },
            )
        }

        Spacer(Modifier.height(18.dp))

        // ────────── SAVE & CONTINUE ──────────
        val ready = manualSsid.isNotBlank() && manualPass.length >= 8
        if (ready) {
            RidgePrimaryButton(
                text = "Save & open channel",
                onClick = { onManualSave(manualSsid.trim(), manualPass) },
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(7.dp))
                    .background(c.bone)
                    .border(BorderStroke(2.5.dp, c.muted), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (manualPass.isNotEmpty() && manualPass.length < 8)
                        "PASSWORD NEEDS 8+ CHARS"
                    else "FILL BOTH FIELDS",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = c.muted,
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // ────────── AUTO ATTEMPT (opt-in, advanced) ──────────
        RidgeLabel("Advanced")
        Spacer(Modifier.height(6.dp))
        AutoBox(state = state, onTryAuto = onTryAuto)

        Spacer(Modifier.height(16.dp))
        RidgeGhostButton("Cancel hosting", onClick = onCancel)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StepRow(num: String, title: String, body: String) {
    val c = RidgeTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(c.hivis)
                .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Text(num, fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.Black,
                 fontSize = 14.sp, color = Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold,
                 fontSize = 14.sp, color = c.ink)
            Text(body, fontFamily = RidgeTheme.type.mono, fontSize = 11.sp,
                 lineHeight = 16.sp, color = c.muted)
        }
    }
}

@Composable
private fun AutoBox(state: HotspotState, onTryAuto: () -> Unit) {
    val c = RidgeTheme.colors
    RidgeCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Try automatic setup",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = c.ink,
            )
            Text(
                "RIDGE asks Android to start a local-only hotspot for you and fills the fields above. Works on Pixel and many phones; some OEMs (Realme / Xiaomi) block this — if so, just use the manual steps above.",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = c.muted,
            )
            Spacer(Modifier.height(10.dp))
            when (state) {
                is HotspotState.Idle -> {
                    RidgeGhostButton("Try automatic", onClick = onTryAuto)
                }
                is HotspotState.Starting -> {
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(7.dp))
                            .background(c.bone)
                            .border(BorderStroke(2.5.dp, c.muted), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("STARTING…", fontFamily = RidgeTheme.type.display,
                             fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = c.muted)
                    }
                }
                is HotspotState.Active -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(c.field))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "RIDGE STARTED A HOTSPOT — FIELDS FILLED ABOVE",
                            fontFamily = RidgeTheme.type.mono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp,
                            color = c.field,
                        )
                    }
                }
                is HotspotState.ManualNeeded -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(c.alarm))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "AUTO BLOCKED BY THIS PHONE — USE MANUAL ABOVE",
                            fontFamily = RidgeTheme.type.mono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp,
                            color = c.alarm,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    RidgeGhostButton("Try again", onClick = onTryAuto)
                }
                is HotspotState.Failed -> {
                    Text(
                        "Failed: ${state.reason}",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 11.sp,
                        color = c.alarm,
                    )
                    Spacer(Modifier.height(6.dp))
                    RidgeGhostButton("Try again", onClick = onTryAuto)
                }
            }
        }
    }
}
