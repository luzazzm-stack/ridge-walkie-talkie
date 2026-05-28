package app.ridge.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.UiState
import app.ridge.ui.components.HazardTape
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeChip
import app.ridge.ui.components.RidgeToggle
import app.ridge.ui.components.Segmented
import app.ridge.ui.theme.RidgeTheme

@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onLeaveGroup: () -> Unit,
    onTheme: (Int) -> Unit,         // 0=Day 1=Night 2=Auto
    onQuality: (Float) -> Unit,
    onSos: (Boolean) -> Unit,
    onSaver: (Int) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onChime: (Boolean) -> Unit,
    onBoost: (Boolean) -> Unit,
    onBtRelay: (Boolean) -> Unit,
) {
    val c = RidgeTheme.colors
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
        if (state.hasGroup) {
            RidgeCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Current group",
                            fontFamily = RidgeTheme.type.mono,
                            fontSize = 10.5.sp,
                            color = c.muted,
                        )
                        Text(
                            state.groupName.ifBlank { "Untitled" },
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = c.ink,
                        )
                        Text(
                            "code ${state.groupCode}",
                            fontFamily = RidgeTheme.type.mono,
                            fontSize = 10.5.sp,
                            color = c.muted,
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(c.alarm)
                            .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                            .clickable { onLeaveGroup() }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        Text(
                            "LEAVE",
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                }
            }
        }

        // Wi-Fi voice quality slider
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Wi-Fi voice quality",
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = c.ink,
                    )
                    Spacer(Modifier.weight(1f))
                    val tag = when {
                        state.voiceQuality < 0.33f -> "RANGE"
                        state.voiceQuality < 0.66f -> "BALANCED"
                        else -> "CRISP"
                    }
                    RidgeChip(tag, color = c.hivisInk)
                }
                Spacer(Modifier.height(11.dp))
                Slider(
                    value = state.voiceQuality,
                    onValueChange = onQuality,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "← MAX RANGE\n~100m · 16kbps",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 10.sp,
                        color = c.muted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "CRISP →\n~40m · HD voice",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 10.sp,
                        color = c.muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(c.ink.copy(alpha = 0.12f)))
                Spacer(Modifier.height(6.dp))
                Text(
                    "BT mode is fixed-bitrate — this slider only affects Wi-Fi.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 10.sp,
                    color = c.muted,
                )
            }
        }

        // Panic SOS
        RidgeCard(Modifier.fillMaxWidth()) {
            Column {
                HazardTape()
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(c.alarm)
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Panic SOS broadcast",
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = c.ink,
                        )
                        Text(
                            "Hold power 3× · alerts all, shares last GPS",
                            fontFamily = RidgeTheme.type.mono,
                            fontSize = 10.5.sp,
                            color = c.muted,
                        )
                    }
                    RidgeToggle(state.sosArmed, onChange = onSos)
                }
            }
        }

        // Theme
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "Theme",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = c.ink,
                )
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("Day", "Night", "Auto"),
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

        // Auto battery saver
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Auto battery saver",
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = c.ink,
                    )
                    Spacer(Modifier.weight(1f))
                    RidgeChip("AT ${state.saverThreshold}%", color = c.ink)
                }
                Spacer(Modifier.height(9.dp))
                Segmented(
                    options = listOf("OFF", "20%", "30%", "50%"),
                    selectedIndex = when (state.saverThreshold) {
                        0 -> 0; 20 -> 1; 30 -> 2; 50 -> 3; else -> 1
                    },
                    onSelect = { onSaver(listOf(0, 20, 30, 50)[it]) },
                )
            }
        }

        // misc toggles
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                ToggleRow("Haptic on transmit", null, state.haptic, onHaptic)
                Divider(c.ink.copy(alpha = 0.12f))
                ToggleRow("Talk-hint chime", "short beep before incoming voice", state.chime, onChime)
                Divider(c.ink.copy(alpha = 0.12f))
                ToggleRow("Voice quality boost", "Opus 24kbps · ~25% more battery", state.boost, onBoost)
                Divider(c.ink.copy(alpha = 0.12f))
                ToggleRow("Bluetooth relay fallback", null, state.btRelay, onBtRelay)
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
            Text(
                title,
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = c.ink,
            )
            if (sub != null) Text(
                sub,
                fontFamily = RidgeTheme.type.mono,
                fontSize = 10.5.sp,
                color = c.muted,
            )
        }
        RidgeToggle(checked, onChange = onChange)
    }
}

@Composable
private fun Divider(color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxWidth().height(2.dp).background(color))
}

@Composable
private fun Slider(value: Float, onValueChange: (Float) -> Unit) {
    val c = RidgeTheme.colors
    val frac = value.coerceIn(0f, 1f)
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        val f = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onValueChange(f)
                    }
                )
            },
    ) {
        val trackWidth = maxWidth
        // track (background)
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.bone)
                .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(8.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(14.dp)
                    .background(c.hivis)
            )
        }
        val knobOffset = (trackWidth * frac) - 15.dp
        val safeOffset = when {
            knobOffset < 0.dp -> 0.dp
            knobOffset > trackWidth - 30.dp -> trackWidth - 30.dp
            else -> knobOffset
        }
        Box(
            Modifier
                .padding(start = safeOffset)
                .align(Alignment.CenterStart)
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(c.ink)
                .border(BorderStroke(3.dp, c.bone), RoundedCornerShape(50))
        )
    }
}
