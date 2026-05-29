package app.ridge.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.components.HazardTape
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeChip
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgeLogo
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.components.RidgeToggle
import app.ridge.ui.theme.RidgeTheme

data class PermStatus(val mic: Boolean, val nearby: Boolean, val notif: Boolean)

@Composable
fun SplashScreen(
    granted: PermStatus,
    onAllow: () -> Unit,
    onOpenAppSettings: () -> Unit = {},
) {
    val c = RidgeTheme.colors

    Column(Modifier.fillMaxSize().background(c.bone)) {

        // brand splash top — ink
        Column(
            Modifier.fillMaxWidth().background(c.ink).padding(top = 36.dp, bottom = 28.dp, start = 26.dp, end = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RidgeLogo(size = 80.dp)
            Text(
                "RIDGE",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                letterSpacing = (-0.7).sp,
                color = c.bone,
            )
            Text(
                "BUILT FOR THE DEAD ZONE",
                fontFamily = RidgeTheme.type.mono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                color = c.hivis,
            )
            Text(
                "v0.11.2",
                fontFamily = RidgeTheme.type.mono,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = c.bone.copy(alpha = 0.5f),
            )
        }
        HazardTape()

        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth()) {
            RidgeLabel("First-run setup · 4 permissions")
            Spacer(Modifier.height(6.dp))
            Text(
                "RIDGE needs\n4 things to work",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 21.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "All offline. Nothing leaves your group. No account, no number.",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = c.muted,
            )
            Spacer(Modifier.height(14.dp))

            PermRow("Microphone", "So your voice transmits", on = granted.mic, accent = c.hivis)
            Spacer(Modifier.height(9.dp))
            PermRow("Nearby devices", "Find your group · Wi-Fi + BT", on = granted.nearby, accent = c.hivis)
            Spacer(Modifier.height(9.dp))
            PermRow("Background activity", "Keep the radio on with screen off", on = true, accent = c.ink, dark = true)
            Spacer(Modifier.height(9.dp))
            PermRow("Notifications", "SOS alerts even when locked", on = granted.notif, accent = c.bone, light = true)

            Spacer(Modifier.height(14.dp))
            RidgePrimaryButton("Allow & continue", onClick = onAllow)
            Spacer(Modifier.height(8.dp))
            Text(
                "Dialog not appearing? Tap below to grant permissions in Android Settings.",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = c.muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                    .background(c.paper)
                    .border(androidx.compose.foundation.BorderStroke(2.5.dp, c.border), androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                    .clickable { onOpenAppSettings() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "OPEN APP SETTINGS",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.6.sp,
                    color = c.ink,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PermRow(title: String, sub: String, on: Boolean, accent: androidx.compose.ui.graphics.Color, dark: Boolean = false, light: Boolean = false) {
    val c = RidgeTheme.colors
    RidgeCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent)
            )
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, color = c.ink)
                Text(sub, fontFamily = RidgeTheme.type.mono, fontSize = 10.5.sp, color = c.muted)
            }
            if (dark || light) {
                RidgeToggle(checked = on) {}
            } else {
                if (on) RidgeChip("✓ ON", color = c.field) else RidgeChip("OFF", color = c.muted)
            }
        }
    }
}
