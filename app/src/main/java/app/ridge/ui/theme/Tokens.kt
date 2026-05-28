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
    val ink: Color,           // primary text + iconography
    val ink2: Color,          // secondary text
    val muted: Color,         // tertiary
    val bone: Color,          // page background
    val paper: Color,         // card surface
    val border: Color,        // card border (less harsh than ink in dark)
    val hivis: Color,         // primary action
    val hivisInk: Color,      // hi-vis used as text
    val beacon: Color,        // attention / range
    val alarm: Color,         // SOS
    val field: Color,         // success / connected
    val bt: Color,            // bluetooth tag
    val isNight: Boolean,
)

val DayColors = RidgeColors(
    ink = Color(0xFF111111),
    ink2 = Color(0xFF3A3934),
    muted = Color(0xFF56554D),
    bone = Color(0xFFF2F0EB),
    paper = Color(0xFFFFFFFF),
    border = Color(0xFF111111),       // hard ink border in day
    hivis = Color(0xFFFF4D1C),
    hivisInk = Color(0xFFC8350C),
    beacon = Color(0xFFFFC400),
    alarm = Color(0xFFE11900),
    field = Color(0xFF1F6F5C),
    bt = Color(0xFF1565C0),
    isNight = false,
)

/* Night palette — overhauled.
   Goals: AMOLED-safe near-black background, clear paper-vs-bone step,
   warm dim borders (NOT cream — reduces visual noise), brighter
   field/bt for legibility on dark, amber primary preserved for night vision. */
val NightColors = RidgeColors(
    ink = Color(0xFFEFEDE4),          // warm cream text
    ink2 = Color(0xFFB9B6AB),         // secondary
    muted = Color(0xFF7C796F),        // tertiary
    bone = Color(0xFF0A0905),          // page bg (warm near-black)
    paper = Color(0xFF1B1814),         // card surface — clearly above bg
    border = Color(0xFF3A352E),        // dim warm border (not cream)
    hivis = Color(0xFFFFB000),         // amber (preserves night vision)
    hivisInk = Color(0xFFFFC85A),     // softer amber for text on dark
    beacon = Color(0xFFF4C242),       // beacon yellow toned down
    alarm = Color(0xFFFF4035),        // alarm red, slightly de-saturated for dark
    field = Color(0xFF52C49C),         // brighter sage green
    bt = Color(0xFF6BA8FF),            // brighter bluetooth blue
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
