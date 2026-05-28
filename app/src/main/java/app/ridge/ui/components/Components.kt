package app.ridge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import app.ridge.ui.theme.RidgeTheme
import kotlinx.coroutines.delay

@Composable
fun RidgeCard(
    modifier: Modifier = Modifier,
    bg: Color? = null,
    border: Color? = null,
    content: @Composable () -> Unit,
) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape
    Box(
        modifier
            .clip(RoundedCornerShape(s.radius))
            .background(bg ?: c.paper)
            .border(BorderStroke(s.border, border ?: c.ink), RoundedCornerShape(s.radius))
    ) { content() }
}

@Composable
fun RidgeChip(
    text: String,
    color: Color? = null,
    bg: Color? = null,
    modifier: Modifier = Modifier,
) {
    val c = RidgeTheme.colors
    val fg = color ?: c.ink
    Box(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg ?: Color.Transparent)
            .border(BorderStroke(2.dp, fg), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            fontFamily = RidgeTheme.type.mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = fg,
        )
    }
}

@Composable
fun RidgeLabel(text: String, color: Color? = null, modifier: Modifier = Modifier) {
    val c = RidgeTheme.colors
    Text(
        text.uppercase(),
        modifier = modifier,
        fontFamily = RidgeTheme.type.mono,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        color = color ?: c.muted,
    )
}

@Composable
fun RidgePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 54.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape
    Box(
        modifier
            .fillMaxWidth()
            .height(minHeight)
            .clip(RoundedCornerShape(s.radius))
            .background(c.hivis)
            .border(BorderStroke(s.border, c.ink), RoundedCornerShape(s.radius))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = if (c.isNight) c.bone else Color.White,
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
fun RidgeGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 54.dp,
) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape
    Box(
        modifier
            .fillMaxWidth()
            .height(minHeight)
            .clip(RoundedCornerShape(s.radius))
            .background(c.paper)
            .border(BorderStroke(s.border, c.ink), RoundedCornerShape(s.radius))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            color = c.ink,
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
fun RidgeToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape
    val bg = if (checked) c.hivis else c.bone
    Box(
        Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(BorderStroke(s.border, c.ink), RoundedCornerShape(16.dp))
            .clickable { onChange(!checked) },
    ) {
        Box(
            Modifier
                .padding(start = if (checked) 23.dp else 2.5.dp, top = 2.5.dp)
                .size(21.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) c.paper else c.ink)
        )
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(s.radius))
            .border(BorderStroke(s.border, c.ink), RoundedCornerShape(s.radius))
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .background(if (active) c.ink else c.paper)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label.uppercase(),
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                    color = if (active) c.bone else c.ink,
                )
            }
            if (i < options.lastIndex) Box(
                Modifier
                    .width(s.border)
                    .height(40.dp)
                    .background(c.ink)
            )
        }
    }
}

@Composable
fun SignalBars(strength: Int, weak: Boolean = false, modifier: Modifier = Modifier) {
    val c = RidgeTheme.colors
    val heights = listOf(6, 9, 12, 16)
    Row(
        modifier.height(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    ) {
        heights.forEachIndexed { i, h ->
            val filled = i < strength
            Box(
                Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .border(BorderStroke(2.dp, c.ink), RoundedCornerShape(1.dp))
                    .background(
                        when {
                            filled && weak -> c.hivis
                            filled -> c.ink
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
fun WaveBars(live: Boolean, bars: Int = 6, modifier: Modifier = Modifier, color: Color? = null) {
    val c = RidgeTheme.colors
    val tick = remember { mutableStateOf(0f) }
    LaunchedEffect(live) {
        while (live) {
            tick.value = (System.currentTimeMillis() % 700) / 700f
            delay(50)
        }
    }
    Row(
        modifier.height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(bars) { i ->
            val phase = (tick.value + i * 0.13f) % 1f
            val frac = if (live) {
                val s = kotlin.math.sin(phase * Math.PI * 2).toFloat()
                0.25f + (s + 1f) * 0.35f
            } else 0.3f
            Box(
                Modifier
                    .width(3.dp)
                    .height((22 * frac).dp)
                    .background(color ?: c.ink, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun Avatar(letter: Char, bg: Color, modifier: Modifier = Modifier) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape
    Box(
        modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(BorderStroke(s.border, c.ink), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter.toString(),
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = c.ink,
        )
    }
}

@Composable
fun HazardTape(modifier: Modifier = Modifier) {
    val c = RidgeTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(c.beacon)
    ) {
        // crude stripe overlay
        Row(Modifier.fillMaxWidth().height(14.dp)) {
            repeat(20) {
                Box(Modifier.weight(1f).height(14.dp).background(if (it % 2 == 0) c.ink else c.beacon))
            }
        }
    }
}

@Composable
fun Divider2(color: Color? = null, modifier: Modifier = Modifier) {
    val c = RidgeTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(RidgeTheme.shape.border)
            .background(color ?: c.ink)
    )
}
