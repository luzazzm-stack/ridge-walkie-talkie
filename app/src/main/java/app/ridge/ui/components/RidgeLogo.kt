package app.ridge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ridge.ui.theme.RidgeTheme

@Composable
fun RidgeLogo(
    size: Dp = 80.dp,
    tile: Boolean = true,
    tileColor: Color? = null,
    arcDark: Color = Color(0xFF111111),
    arcLight: Color = Color(0xFFFFFFFF),
    modifier: Modifier = Modifier,
) {
    val colors = RidgeTheme.colors
    val tileFill = tileColor ?: colors.hivis
    val base = if (tile) {
        modifier.size(size).clip(RoundedCornerShape(percent = 22)).background(tileFill)
    } else {
        modifier.size(size)
    }
    Box(base, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = this.size.width / 100f
            val stroke = Stroke(width = 6.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(arcDark, radius = 6.5f * s, center = Offset(50f * s, 74f * s))
            val p1 = Path().apply {
                moveTo(37f * s, 62f * s); lineTo(50f * s, 49f * s); lineTo(63f * s, 62f * s)
            }
            drawPath(p1, arcDark, style = stroke)
            val p2 = Path().apply {
                moveTo(28f * s, 60f * s); lineTo(50f * s, 38f * s); lineTo(72f * s, 60f * s)
            }
            drawPath(p2, arcDark, style = stroke)
            val p3 = Path().apply {
                moveTo(20f * s, 58f * s); lineTo(50f * s, 28f * s); lineTo(80f * s, 58f * s)
            }
            drawPath(p3, if (tile) arcLight else arcDark, style = stroke)
        }
    }
}
