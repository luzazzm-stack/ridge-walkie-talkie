package app.ridge.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.components.Segmented
import app.ridge.ui.theme.RidgeTheme

@Composable
fun GroupSetupScreen(
    code: String = "4417",
    onJoin: () -> Unit,
    onBack: () -> Unit,
) {
    val c = RidgeTheme.colors
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(c.bone).padding(horizontal = 22.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper).border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Text(
                "Join a group",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
        }
        Spacer(Modifier.height(16.dp))
        Segmented(
            options = listOf("Scan QR", "Enter code"),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        Spacer(Modifier.height(18.dp))

        if (tab == 0) {
            RidgeCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // QR placeholder (visual proxy)
                    QrPlaceholder()
                    Text(
                        "Point at a friend's My QR screen —\njoins instantly, even offline.",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = c.muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(2.dp).background(c.ink.copy(alpha = 0.25f)))
                Spacer(Modifier.size(12.dp))
                RidgeLabel("OR SHARE A CODE")
                Spacer(Modifier.size(12.dp))
                Box(Modifier.weight(1f).height(2.dp).background(c.ink.copy(alpha = 0.25f)))
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            code.toCharArray().forEachIndexed { i, ch ->
                val highlight = i == code.length - 1
                Box(
                    Modifier
                        .size(width = 56.dp, height = 66.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(c.paper)
                        .border(BorderStroke(2.5.dp, if (highlight) c.hivis else c.ink), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ch.toString(),
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = c.ink,
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        RidgePrimaryButton("Join group", onClick = onJoin)
    }
}

@Composable
private fun QrPlaceholder() {
    val c = RidgeTheme.colors
    Box(
        Modifier
            .size(178.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(c.paper)
            .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Crude QR-feel: alternating squares grid via Row/Column. Replaced with real generator later.
        Column(
            Modifier.padding(10.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(15) { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(15) { col ->
                        val on = ((r * 7 + col * 13 + r * col) % 3) == 0 ||
                                (r in 0..3 && col in 0..3) ||
                                (r in 0..3 && col in 11..14) ||
                                (r in 11..14 && col in 0..3)
                        Box(
                            Modifier
                                .size(9.dp)
                                .background(if (on) c.ink else c.paper)
                        )
                    }
                }
            }
        }
    }
}
