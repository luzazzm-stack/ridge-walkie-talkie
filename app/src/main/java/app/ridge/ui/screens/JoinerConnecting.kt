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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.JoinState
import app.ridge.ui.components.HazardTape
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun JoinerConnectingScreen(
    state: JoinState,
    targetSsid: String,
    targetPass: String,
    onOpenWifiSettings: () -> Unit,
    onTryConnect: () -> Unit,
    onConnected: () -> Unit,
    onCancel: () -> Unit,
) {
    val c = RidgeTheme.colors

    if (state is JoinState.Connected) {
        LaunchedEffect(Unit) { onConnected() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bone)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        // header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper)
                    .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(8.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Text(
                "Join host's Wi-Fi",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
        }

        Spacer(Modifier.height(20.dp))

        // What to look for
        if (targetSsid.isNotBlank()) {
            RidgeCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    RidgeLabel("You're looking for")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        targetSsid,
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = c.ink,
                    )
                    if (targetPass.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        RidgeLabel("Password")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            targetPass,
                            fontFamily = RidgeTheme.type.mono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = c.ink,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 3 steps
        RidgeCard(Modifier.fillMaxWidth()) {
            Column {
                HazardTape()
                Column(Modifier.padding(16.dp)) {
                    StepRow("1", "Open Wi-Fi settings",
                            "Tap the orange button below — Android Wi-Fi screen opens.")
                    Spacer(Modifier.height(10.dp))
                    StepRow("2", "Tap the host's hotspot",
                            if (targetSsid.isNotBlank())
                                "Look for \"$targetSsid\" in the list. Tap → type the password above."
                            else "Pick the hotspot your friend turned on. Type its password.")
                    Spacer(Modifier.height(10.dp))
                    StepRow("3", "Come back, tap Try connecting",
                            "Android will say the network has no internet — tap \"Keep using\" or \"Connect anyway\". Return to RIDGE, then the grey button.")
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // STATE BLOCK
        when (state) {
            is JoinState.Disconnected -> { /* nothing shown, just the buttons below */ }
            is JoinState.Connecting -> {
                StatusLine(color = c.beacon, label = "CHECKING…", body = "Looking for your Wi-Fi gateway.")
                Spacer(Modifier.height(12.dp))
            }
            is JoinState.Connected -> {
                StatusLine(
                    color = c.field,
                    label = "CONNECTED — opening channel…",
                    body = "Gateway ${state.gatewayIp}",
                )
                Spacer(Modifier.height(12.dp))
            }
            is JoinState.Failed -> {
                StatusLine(
                    color = c.alarm,
                    label = "NOT CONNECTED",
                    body = state.reason,
                )
                if (state.diagnostics.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    RidgeCard(Modifier.fillMaxWidth(), bg = c.paper) {
                        Column(Modifier.padding(12.dp)) {
                            RidgeLabel("What RIDGE saw")
                            Spacer(Modifier.height(4.dp))
                            state.diagnostics.forEach { d ->
                                Text(
                                    "· $d",
                                    fontFamily = RidgeTheme.type.mono,
                                    fontSize = 10.5.sp,
                                    color = c.muted,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Action buttons
        RidgePrimaryButton("Open Wi-Fi settings", onClick = onOpenWifiSettings)
        Spacer(Modifier.height(10.dp))
        RidgeGhostButton(
            if (state is JoinState.Failed) "Try again" else "Try connecting",
            onClick = onTryConnect,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Step 1: tap Open Wi-Fi settings to join the hotspot. Step 2: come back and tap Try connecting.",
            fontFamily = RidgeTheme.type.mono,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            color = c.muted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))
        RidgeGhostButton("Cancel", onClick = onCancel)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatusLine(color: androidx.compose.ui.graphics.Color, label: String, body: String) {
    val c = RidgeTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
        Column {
            Text(
                label,
                fontFamily = RidgeTheme.type.mono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.0.sp,
                color = color,
            )
            Text(
                body,
                fontFamily = RidgeTheme.type.mono,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = c.ink,
            )
        }
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
