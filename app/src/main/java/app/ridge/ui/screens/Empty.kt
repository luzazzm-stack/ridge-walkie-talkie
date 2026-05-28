package app.ridge.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.components.RidgeChip
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLogo
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun EmptyScreen(
    onStart: () -> Unit,
    onJoin: () -> Unit,
) {
    val c = RidgeTheme.colors

    Column(
        Modifier.fillMaxSize().background(c.bone).padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RidgeLogo(size = 32.dp)
            Spacer(Modifier.size(10.dp))
            Text("RIDGE", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.Black, fontSize = 21.sp, color = c.ink)
            Spacer(Modifier.weight(1f))
            RidgeChip("WORKS OFFLINE", color = c.ink)
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val t = rememberInfiniteTransition(label = "breathe")
            val a1 by t.animateFloat(0.22f, 0.45f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "a1")
            val a2 by t.animateFloat(0.22f, 0.45f, infiniteRepeatable(tween(2000, delayMillis = 300), RepeatMode.Reverse), label = "a2")

            Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(160.dp).alpha(a1).border(BorderStroke(2.5.dp, c.ink), CircleShape))
                Box(Modifier.size(108.dp).alpha(a2).border(BorderStroke(2.5.dp, c.ink), CircleShape))
                RidgeLogo(size = 52.dp, tile = false)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "No group yet",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                letterSpacing = (-0.5).sp,
                color = c.ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Start a channel for your trek, or join one\nthat's already on the air nearby.",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = c.muted,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            RidgePrimaryButton("+ Start a group", onClick = onStart, minHeight = 60.dp, fontSize = 16.sp)
            RidgeGhostButton("Join with code or QR", onClick = onJoin, minHeight = 60.dp)
            Text(
                "Scanning for nearby groups…",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 11.sp,
                color = c.muted,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
