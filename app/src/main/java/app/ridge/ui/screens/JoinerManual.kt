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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.components.HazardTape
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun JoinerManualScreen(
    onConnect: (ssid: String, pass: String) -> Unit,
    onCancel: () -> Unit,
) {
    val c = RidgeTheme.colors
    var ssid by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bone)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper)
                    .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(8.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Text(
                "Type host's hotspot",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
        }
        Spacer(Modifier.height(20.dp))

        RidgeCard(Modifier.fillMaxWidth()) {
            Column {
                HazardTape()
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Ask the host",
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = c.ink,
                    )
                    Text(
                        "The code you entered doesn't carry the hotspot name + password. Ask the host to read them aloud, or use Scan QR instead — that auto-fills.",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = c.muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        RidgeLabel("Host's hotspot name")
        Spacer(Modifier.height(6.dp))
        RidgeCard(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = ssid,
                onValueChange = { if (it.length <= 32) ssid = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = c.ink,
                ),
                cursorBrush = SolidColor(c.hivis),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                decorationBox = { inner ->
                    if (ssid.isEmpty()) Text(
                        "e.g. AndroidAP1234",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = c.muted.copy(alpha = 0.5f),
                    )
                    inner()
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        RidgeLabel("Host's password")
        Spacer(Modifier.height(6.dp))
        RidgeCard(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = pass,
                onValueChange = { if (it.length <= 64) pass = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = c.ink,
                ),
                cursorBrush = SolidColor(c.hivis),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                decorationBox = { inner ->
                    if (pass.isEmpty()) Text(
                        "hotspot password",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = c.muted.copy(alpha = 0.5f),
                    )
                    inner()
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        val ready = ssid.isNotBlank() && pass.length >= 8
        if (ready) {
            RidgePrimaryButton("Connect", onClick = { onConnect(ssid.trim(), pass) })
        } else {
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(7.dp))
                    .background(c.bone)
                    .border(BorderStroke(2.5.dp, c.muted), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (pass.isNotEmpty() && pass.length < 8) "PASSWORD NEEDS 8+ CHARS"
                    else "FILL BOTH FIELDS",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = c.muted,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        RidgeGhostButton("Cancel", onClick = onCancel)
        Spacer(Modifier.height(8.dp))
    }
}
