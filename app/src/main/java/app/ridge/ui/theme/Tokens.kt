package app.ridge.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class RidgeColors(
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val bone: Color,
    val paper: Color,
    val hivis: Color,
    val hivisInk: Color,
    val beacon: Color,
    val alarm: Color,
    val field: Color,
    val bt: Color,
    val isNight: Boolean,
)

val DayColors = RidgeColors(
    ink = Color(0xFF111111),
    ink2 = Color(0xFF3A3934),
    muted = Color(0xFF56554D),
    bone = Color(0xFFF2F0EB),
    paper = Color(0xFFFFFFFF),
    hivis = Color(0xFFFF4D1C),
    hivisInk = Color(0xFFC8350C),
    beacon = Color(0xFFFFC400),
    alarm = Color(0xFFE11900),
    field = Color(0xFF1F6F5C),
    bt = Color(0xFF1565C0),
    isNight = false,
)

val NightColors = RidgeColors(
    ink = Color(0xFFF2F0EB),
    ink2 = Color(0xFFC8C6BE),
    muted = Color(0xFF88857B),
    bone = Color(0xFF0A0907),
    paper = Color(0xFF15130F),
    hivis = Color(0xFFFFB300),   // amber preserves night vision
    hivisInk = Color(0xFFFFB300),
    beacon = Color(0xFFFFC400),
    alarm = Color(0xFFE11900),
    field = Color(0xFF1F6F5C),
    bt = Color(0xFF1565C0),
    isNight = true,
)

@Immutable
data class RidgeShape(
    val radius: androidx.compose.ui.unit.Dp = 7.dp,
    val radiusLg: androidx.compose.ui.unit.Dp = 14.dp,
    val border: androidx.compose.ui.unit.Dp = 2.5.dp,
    val borderHeavy: androidx.compose.ui.unit.Dp = 3.5.dp,
)

@Immutable
data class RidgeType(
    val display: FontFamily = FontFamily.SansSerif,
    val mono: FontFamily = FontFamily.Monospace,
)

val LocalRidgeColors = staticCompositionLocalOf { DayColors }
val LocalRidgeShape = staticCompositionLocalOf { RidgeShape() }
val LocalRidgeType = staticCompositionLocalOf { RidgeType() }

object RidgeTheme {
    val colors: RidgeColors @Composable get() = LocalRidgeColors.current
    val shape: RidgeShape @Composable get() = LocalRidgeShape.current
    val type: RidgeType @Composable get() = LocalRidgeType.current
}

@Composable
fun RidgeTheme(night: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (night) NightColors else DayColors
    CompositionLocalProvider(
        LocalRidgeColors provides colors,
        LocalRidgeShape provides RidgeShape(),
        LocalRidgeType provides RidgeType(),
        content = content,
    )
}
