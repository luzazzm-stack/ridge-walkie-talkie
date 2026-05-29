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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.UiState
import app.ridge.ui.components.HazardTape
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeToggle
import app.ridge.ui.components.Segmented
import app.ridge.ui.theme.RidgeTheme

@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onLeaveGroup: () -> Unit,
    onStopApp: () -> Unit,
    onSaveName: (String) -> Unit,
    onTheme: (Int) -> Unit,         // 0=Day 1=Night
    onSos: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onChime: (Boolean) -> Unit,
) {
    val c = RidgeTheme.colors
    var name by remember(state.myName) { mutableStateOf(state.myName) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bone)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper)
                    .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(8.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Text(
                "Settings",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp,
                color = c.ink,
            )
        }

        // ── Your name ──
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Your name",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = c.ink,
                )
                Text(
                    "Shown to others in the group.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 10.5.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(7.dp))
                            .background(c.bone)
                            .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        BasicTextField(
                            value = name,
                            onValueChange = { if (it.length <= 24) name = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = RidgeTheme.type.display,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = c.ink,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveName(name) }),
                            cursorBrush = SolidColor(c.hivis),
                            decorationBox = { inner ->
                                if (name.isEmpty()) Text(
                                    "e.g. Pemba",
                                    fontFamily = RidgeTheme.type.display,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = c.muted.copy(alpha = 0.5f),
                                )
                                inner()
                            },
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(c.hivis)
                            .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                            .clickable { onSaveName(name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            "SAVE",
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // ── Current group + leave ──
        if (state.hasGroup) {
            RidgeCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Current group", fontFamily = RidgeTheme.type.mono, fontSize = 10.5.sp, color = c.muted)
                        Text(
                            state.groupName.ifBlank { "Untitled" },
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = c.ink,
                        )
                        Text("code ${state.groupCode}", fontFamily = RidgeTheme.type.mono, fontSize = 10.5.sp, color = c.muted)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(c.alarm)
                            .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                            .clickable { onLeaveGroup() }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        Text("LEAVE", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }

        // ── Panic SOS ──
        RidgeCard(Modifier.fillMaxWidth()) {
            Column {
                HazardTape()
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(c.alarm))
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Panic SOS", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.ink)
                        Text(
                            "Full-screen alert on your phone. (Network broadcast to others coming soon.)",
                            fontFamily = RidgeTheme.type.mono,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            color = c.muted,
                        )
                    }
                    RidgeToggle(state.sosArmed, onChange = onSos)
                }
            }
        }

        // ── Theme ──
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Theme", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.ink)
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("Day", "Night"),
                    selectedIndex = if (state.night) 1 else 0,
                    onSelect = onTheme,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Night uses amber to preserve dark-adapted vision.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 10.5.sp,
                    color = c.muted,
                )
            }
        }

        // ── Toggles that actually do something ──
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                ToggleRow("Haptic on transmit", "buzz when you start talking", state.haptic, onHaptic)
                Divider(c.ink.copy(alpha = 0.12f))
                ToggleRow("Talk-hint chime", "short beep before incoming voice", state.chime, onChime)
            }
        }

        // ── Stop RIDGE ──
        RidgeCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Stop RIDGE", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.ink)
                    Text(
                        "Quit the radio, stop the background service and remove the notification.",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = c.muted,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(c.ink)
                        .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                        .clickable { onStopApp() }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                ) {
                    Text("STOP", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = c.bone)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ToggleRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = RidgeTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = c.ink)
            if (sub != null) Text(sub, fontFamily = RidgeTheme.type.mono, fontSize = 10.5.sp, color = c.muted)
        }
        RidgeToggle(checked, onChange = onChange)
    }
}

@Composable
private fun Divider(color: Color) {
    Box(Modifier.fillMaxWidth().height(2.dp).background(color))
}
