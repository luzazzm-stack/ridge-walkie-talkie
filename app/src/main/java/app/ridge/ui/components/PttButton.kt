package app.ridge.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.theme.RidgeTheme

@Composable
fun PttButton(
    talking: Boolean,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape

    val offsetY by animateDpAsState(if (talking) 5.dp else 0.dp, tween(70), label = "ptt-offset")
    val shadowH by animateDpAsState(if (talking) 3.dp else 8.dp, tween(70), label = "ptt-shadow")

    Box(modifier.size(220.dp), contentAlignment = Alignment.Center) {
        // outer concentric rings
        Box(Modifier.size(220.dp).clip(CircleShape).border(BorderStroke(2.5.dp, c.ink.copy(alpha = 0.08f)), CircleShape))
        Box(Modifier.size(196.dp).clip(CircleShape).border(BorderStroke(2.5.dp, c.ink.copy(alpha = 0.16f)), CircleShape))

        // broadcast pulses
        if (talking) {
            val t = rememberInfiniteTransition(label = "pulse")
            val scale1 by t.animateFloat(1f, 1.9f, infiniteRepeatable(tween(1500), initialStartOffset = androidx.compose.animation.core.StartOffset(0)), label = "p1")
            val scale2 by t.animateFloat(1f, 1.9f, infiniteRepeatable(tween(1500), initialStartOffset = androidx.compose.animation.core.StartOffset(500)), label = "p2")
            val alpha1 by t.animateFloat(0.55f, 0f, infiniteRepeatable(tween(1500), initialStartOffset = androidx.compose.animation.core.StartOffset(0)), label = "a1")
            val alpha2 by t.animateFloat(0.55f, 0f, infiniteRepeatable(tween(1500), initialStartOffset = androidx.compose.animation.core.StartOffset(500)), label = "a2")
            Box(
                Modifier.size(184.dp).clip(CircleShape)
                    .graphicsLayer { scaleX = scale1; scaleY = scale1; alpha = alpha1 }
                    .border(BorderStroke(3.dp, c.hivis), CircleShape)
            )
            Box(
                Modifier.size(184.dp).clip(CircleShape)
                    .graphicsLayer { scaleX = scale2; scaleY = scale2; alpha = alpha2 }
                    .border(BorderStroke(3.dp, c.hivis), CircleShape)
            )
        }

        // shadow lip (offset down so the "drop" reveals when pressed)
        Box(
            Modifier
                .size(184.dp)
                .offset(y = shadowH)
                .clip(CircleShape)
                .background(c.ink)
        )

        // main button
        Box(
            Modifier
                .size(184.dp)
                .offset(y = offsetY)
                .clip(CircleShape)
                .background(if (talking) c.hivis else c.paper)
                .border(BorderStroke(s.borderHeavy, c.ink), CircleShape)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val anyDown = ev.changes.any { it.pressed }
                            onPressChange(anyDown)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(if (talking) c.paper else c.hivis)
                        .border(BorderStroke(s.border, c.ink), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    // mic icon glyph
                    Box(
                        Modifier
                            .size(width = 12.dp, height = 18.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .background(if (talking) c.hivis else c.paper)
                    )
                }
                Text(
                    if (talking) "ON AIR" else "HOLD TO TALK",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (talking) c.paper else c.ink,
                )
                Text(
                    if (talking) "RELEASE TO STOP" else "PRESS & HOLD",
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = if (talking) c.paper.copy(alpha = 0.85f) else c.muted,
                )
            }
        }
    }
}
