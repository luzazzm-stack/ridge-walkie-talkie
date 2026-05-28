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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.theme.RidgeTheme

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    cancelLabel: String = "Cancel",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = RidgeTheme.colors

    Box(Modifier.fillMaxSize()) {
        // scrim
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x99111111))
                .clickable { onDismiss() }
        )
        // dialog
        Box(
            Modifier
                .padding(horizontal = 28.dp)
                .align(Alignment.Center)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.paper)
                .border(BorderStroke(3.dp, c.border), RoundedCornerShape(10.dp)),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    title,
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp,
                    color = c.ink,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    body,
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = c.muted,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // cancel (ghost)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(c.bone)
                            .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            cancelLabel.uppercase(),
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = c.ink,
                        )
                    }
                    // confirm (primary or danger)
                    Box(
                        Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (danger) c.alarm else c.hivis)
                            .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            confirmLabel.uppercase(),
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
