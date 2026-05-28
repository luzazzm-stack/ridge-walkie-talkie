package app.ridge.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.components.Segmented
import app.ridge.ui.theme.RidgeTheme

@Composable
fun GroupSetupScreen(
    onScanQr: () -> Unit,
    onJoinWithCode: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = RidgeTheme.colors
    var tab by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(c.bone).padding(horizontal = 22.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper).border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(8.dp))
                    .clickable { onBack() },
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.paper)
                            .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("📷", fontSize = 64.sp)
                    }
                    Text(
                        "Point at a friend's RIDGE QR",
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = c.ink,
                    )
                    Text(
                        "Ask them to open RIDGE → Invite. You'll see their QR — scan it to join their group.",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = c.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            RidgePrimaryButton("Open camera to scan", onClick = onScanQr)
        } else {
            RidgeLabel("Enter the 4-digit group code")
            Spacer(Modifier.height(8.dp))
            RidgeCard(Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = code,
                    onValueChange = { new -> if (new.length <= 4 && new.all { it.isDigit() }) code = new },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = c.ink,
                        textAlign = TextAlign.Center,
                        letterSpacing = 12.sp,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (code.length == 4) onJoinWithCode(code)
                    }),
                    cursorBrush = SolidColor(c.hivis),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 22.dp),
                    decorationBox = { inner ->
                        if (code.isEmpty()) {
                            Text(
                                "1234",
                                fontFamily = RidgeTheme.type.mono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = c.muted.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center,
                                letterSpacing = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Your friend can read this aloud over their radio or share it via text.",
                fontFamily = RidgeTheme.type.mono,
                fontSize = 11.sp,
                color = c.muted,
            )
            Spacer(Modifier.height(18.dp))
            if (code.length == 4) {
                RidgePrimaryButton("Join group", onClick = { onJoinWithCode(code) })
            } else {
                Box(
                    Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(7.dp))
                        .background(c.bone)
                        .border(BorderStroke(2.5.dp, c.muted), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "ENTER 4 DIGITS",
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = c.muted,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        RidgeGhostButton("Cancel", onClick = onBack)
        Spacer(Modifier.height(8.dp))
    }
}
