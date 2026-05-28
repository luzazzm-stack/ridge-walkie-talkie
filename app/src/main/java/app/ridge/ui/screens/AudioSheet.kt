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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.AudioOut
import app.ridge.core.UiState
import app.ridge.ui.components.RidgeChip
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun AudioSheet(
    state: UiState,
    onPick: (AudioOut) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = RidgeTheme.colors

    Box(Modifier.fillMaxSize()) {
        // scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x73111111))
                .clickable(onClick = onDismiss)
        )
        // sheet
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(c.paper)
                .border(
                    BorderStroke(3.5.dp, c.ink),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            // handle
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.ink.copy(alpha = 0.4f))
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Audio output",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp,
                    color = c.ink,
                )
                RidgeLabel("CHOOSE ONE")
            }

            OutRow(
                title = "Phone earpiece",
                sub = "private · for 1-on-1 answers",
                selected = state.audioOut == AudioOut.Earpiece,
                onClick = { onPick(AudioOut.Earpiece) },
            )
            OutRow(
                title = "Loudspeaker",
                sub = "default · group voice",
                selected = state.audioOut == AudioOut.Speaker,
                onClick = { onPick(AudioOut.Speaker) },
            )
            OutRow(
                title = state.btHeadsetName ?: "Bluetooth headset",
                sub = if (state.btHeadsetName != null)
                    "bluetooth · battery ${state.btHeadsetBattery ?: "—"}%"
                else "no headset paired",
                selected = state.audioOut == AudioOut.BluetoothHeadset,
                onClick = { onPick(AudioOut.BluetoothHeadset) },
                disabled = state.btHeadsetName == null,
                iconBg = c.bt,
            )
            OutRow(
                title = "Wired headphones",
                sub = if (state.wiredPluggedIn) "ready" else "not plugged in",
                selected = state.audioOut == AudioOut.Wired,
                onClick = { onPick(AudioOut.Wired) },
                disabled = !state.wiredPluggedIn,
            )

            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RidgeGhostButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f), minHeight = 48.dp)
                RidgePrimaryButton("Done", onClick = onDismiss, modifier = Modifier.weight(1.4f), minHeight = 48.dp, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OutRow(
    title: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit,
    disabled: Boolean = false,
    iconBg: Color? = null,
) {
    val c = RidgeTheme.colors
    val rowBg = when {
        selected -> c.ink
        else -> c.bone
    }
    val fg = if (selected) c.bone else c.ink
    Row(
        Modifier
            .fillMaxWidth()
            .alphaIf(disabled, alphaVal = 0.4f)
            .clip(RoundedCornerShape(7.dp))
            .background(rowBg)
            .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(7.dp))
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg ?: c.paper)
                .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(8.dp))
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = fg,
            )
            Text(
                sub,
                fontFamily = RidgeTheme.type.mono,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                color = if (selected) c.bone.copy(alpha = 0.85f) else c.muted,
            )
        }
        val tag = when {
            disabled -> "—"
            selected -> "ON"
            else -> "OFF"
        }
        RidgeChip(
            tag,
            color = if (selected) c.bone else c.ink,
        )
    }
}

private fun Modifier.alphaIf(condition: Boolean, alphaVal: Float): Modifier =
    if (condition) this.alpha(alphaVal) else this
