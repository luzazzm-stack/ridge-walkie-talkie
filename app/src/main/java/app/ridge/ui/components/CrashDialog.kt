package app.ridge.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.theme.RidgeTheme

@Composable
fun CrashDialog(
    trace: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = RidgeTheme.colors
    val clipboard = LocalClipboardManager.current

    Box(Modifier.fillMaxSize().background(Color(0xCC111111))) {
        Column(
            Modifier
                .padding(20.dp)
                .align(Alignment.Center)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.paper)
                .border(BorderStroke(3.dp, c.alarm), RoundedCornerShape(10.dp))
                .padding(18.dp),
        ) {
            Text(
                "RIDGE crashed last time",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = c.alarm,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Screenshot this and send it so the exact line can be fixed.",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 11.sp,
                color = c.muted,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(c.bone)
                    .border(BorderStroke(2.dp, c.border), RoundedCornerShape(7.dp))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    trace,
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp,
                    color = c.ink,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(7.dp))
                        .background(c.bone)
                        .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                        .clickable { clipboard.setText(AnnotatedString(trace)); onCopy() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("COPY", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.ink)
                }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(7.dp))
                        .background(c.ink)
                        .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("DISMISS", fontFamily = RidgeTheme.type.display, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = c.bone)
                }
            }
        }
    }
}
