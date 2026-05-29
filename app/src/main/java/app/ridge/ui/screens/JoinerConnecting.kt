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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.JoinState
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun JoinerConnectingScreen(
    state: JoinState,
    targetSsid: String,
    onRetry: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper)
                    .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(8.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Text(
                "Joining",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            targetSsid.ifBlank { "RIDGE group" },
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp,
            color = c.ink,
        )
        Spacer(Modifier.height(8.dp))

        when (state) {
            is JoinState.Disconnected -> {
                Text(
                    "Ready to connect.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(20.dp))
                RidgePrimaryButton("Connect", onClick = onRetry)
            }
            is JoinState.Connecting -> {
                Text(
                    "Asking Android to connect. Tap “Connect” if a system dialog appears.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(20.dp))
                RidgeCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Connecting…",
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = c.ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Joining ${state.ssid}",
                            fontFamily = RidgeTheme.type.mono,
                            fontSize = 11.sp,
                            color = c.muted,
                        )
                    }
                }
            }
            is JoinState.Connected -> {
                RidgeCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(c.field))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "CONNECTED",
                                fontFamily = RidgeTheme.type.mono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.3.sp,
                                color = c.field,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        RidgeLabel("Gateway")
                        Text(
                            state.gatewayIp,
                            fontFamily = RidgeTheme.type.mono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = c.ink,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Entering the channel…",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 11.5.sp,
                    color = c.muted,
                )
            }
            is JoinState.Failed -> {
                Text(
                    state.reason,
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = c.alarm,
                )
                Spacer(Modifier.height(20.dp))
                RidgePrimaryButton("Retry", onClick = onRetry)
            }
        }
        Spacer(Modifier.weight(1f))
        RidgeGhostButton("Cancel", onClick = onCancel)
        Spacer(Modifier.height(8.dp))
    }
}
