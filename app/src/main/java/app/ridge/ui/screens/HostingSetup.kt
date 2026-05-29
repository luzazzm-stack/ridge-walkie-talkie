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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.HotspotState
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun HostingSetupScreen(
    groupName: String,
    state: HotspotState,
    onRetry: () -> Unit,
    onManualSave: (ssid: String, pass: String) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = RidgeTheme.colors
    var manualSsid by remember { mutableStateOf("") }
    var manualPass by remember { mutableStateOf("") }

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
            Text(
                "Hosting setup",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            groupName.ifBlank { "Untitled" },
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp,
            color = c.ink,
        )
        Spacer(Modifier.height(8.dp))

        when (state) {
            is HotspotState.Idle, is HotspotState.Starting -> {
                Text(
                    "Starting your offline Wi-Fi hotspot. Android may ask you to allow this.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(20.dp))
                RidgeCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Starting…",
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = c.ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Allow the Android prompt if it appears.",
                            fontFamily = RidgeTheme.type.mono,
                            fontSize = 11.sp,
                            color = c.muted,
                        )
                    }
                }
            }
            is HotspotState.Active -> {
                Text(
                    "Hotspot is live. Tap continue to open your channel — friends can join by scanning your QR or typing the code.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(20.dp))
                RidgeCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(c.field))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "HOTSPOT ACTIVE",
                                fontFamily = RidgeTheme.type.mono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.3.sp,
                                color = c.field,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        RidgeLabel("Network name")
                        Text(
                            state.ssid,
                            fontFamily = RidgeTheme.type.mono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = c.ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        RidgeLabel("Password")
                        Text(
                            state.passphrase.ifBlank { "(no password)" },
                            fontFamily = RidgeTheme.type.mono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = c.ink,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                RidgePrimaryButton("Continue to channel", onClick = onContinue)
            }
            is HotspotState.ManualNeeded -> {
                Text(
                    "Local hotspot isn't available. Turn on Mobile Hotspot in Android settings, then type its name and password here.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(18.dp))
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
                                "e.g. My Phone",
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
                Spacer(Modifier.height(20.dp))
                if (manualSsid.isNotBlank() && manualPass.isNotBlank()) {
                    RidgePrimaryButton("Save & continue", onClick = { onManualSave(manualSsid, manualPass) })
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(7.dp))
                            .background(c.bone)
                            .border(BorderStroke(2.5.dp, c.muted), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "FILL BOTH FIELDS",
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = c.muted,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                RidgeGhostButton("Try local-only again", onClick = onRetry)
            }
            is HotspotState.Failed -> {
                Text(
                    "Couldn't start. ${state.reason}",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = c.alarm,
                )
                Spacer(Modifier.height(20.dp))
                RidgePrimaryButton("Try again", onClick = onRetry)
            }
        }
        Spacer(Modifier.weight(1f))
        RidgeGhostButton("Cancel hosting", onClick = onCancel)
        Spacer(Modifier.height(8.dp))
    }
}
