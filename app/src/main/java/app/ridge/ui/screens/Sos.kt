package app.ridge.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.theme.RidgeTheme

@Composable
fun SosScreen(
    fromName: String,
    onRespond: () -> Unit,
    onClear: () -> Unit,
    onFalseAlarm: () -> Unit,
) {
    val c = RidgeTheme.colors
    val alarm = Color(0xFFE11900)
    val alarmDim = Color(0xFFB31300)
    val t = rememberInfiniteTransition(label = "sos")
    val bg by t.animateColor(
        alarm, alarmDim,
        infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "bg"
    )

    Column(
        Modifier.fillMaxSize().background(bg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SOS LIVE · 14:32", fontFamily = RidgeTheme.type.mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text("62%", fontFamily = RidgeTheme.type.mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
        }

        Spacer(Modifier.weight(1f))

        // alert glyph w/ rings
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(140.dp).clip(CircleShape).border(BorderStroke(4.dp, Color.White.copy(alpha = 0.5f)), CircleShape))
            Box(Modifier.size(104.dp).clip(CircleShape).border(BorderStroke(4.dp, Color.White.copy(alpha = 0.3f)), CircleShape))
            Text("⚠", fontSize = 56.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "SOS",
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.Black,
            fontSize = 42.sp,
            letterSpacing = 1.7.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "$fromName needs help",
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Broadcasting to all 7 members\nLast GPS · 28.5310°N 83.8210°E\n↑ 4,130m · 2 min ago",
            fontFamily = RidgeTheme.type.mono,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Auto-clears if no response in 14:32",
            fontFamily = RidgeTheme.type.mono,
            fontSize = 10.5.sp,
            color = Color.White.copy(alpha = 0.75f),
        )

        Spacer(Modifier.weight(1f))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // white button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White)
                    .clickable(onClick = onRespond),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "RESPOND — I'M COMING",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = alarm,
                )
            }
            // outlined button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.Transparent)
                    .border(BorderStroke(2.5.dp, Color.White), RoundedCornerShape(7.dp))
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "MARK RESOLVED / CLEAR",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color.White,
                )
            }
            Text(
                "False alarm — only I see this",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp).clickable(onClick = onFalseAlarm),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
