package app.ridge.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.components.QrCode
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun InviteScreen(
    groupName: String,
    groupCode: String,
    onBack: () -> Unit,
) {
    val c = RidgeTheme.colors
    val ctx = LocalContext.current
    val payload = "ridge:$groupCode|$groupName"

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
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Text(
                "Invite people",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Two ways to join",
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            letterSpacing = (-0.5).sp,
            color = c.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Show this QR for them to scan, or read the code aloud over the radio.",
            fontFamily = RidgeTheme.type.mono,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = c.muted,
        )

        Spacer(Modifier.height(18.dp))
        RidgeLabel("My QR")
        Spacer(Modifier.height(8.dp))
        RidgeCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QrCode(text = payload, size = 200.dp)
                Text(
                    groupName.ifBlank { "Untitled" },
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = c.ink,
                )
                Text(
                    "Have a friend open RIDGE → Join → Scan QR.",
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 11.sp,
                    color = c.muted,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        RidgeLabel("Or share the 4-digit code")
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            groupCode.padStart(4, '0').take(4).forEach { ch ->
                Box(
                    Modifier
                        .size(width = 56.dp, height = 70.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(c.paper)
                        .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ch.toString(),
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = c.ink,
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        RidgePrimaryButton(
            text = "Share invite",
            onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Join my RIDGE group · $groupName"
                    )
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Join my RIDGE walkie-talkie:\n\n" +
                                "Group: ${groupName.ifBlank { "Untitled" }}\n" +
                                "Code: $groupCode\n\n" +
                                "1. Install RIDGE\n" +
                                "2. Open the app → Join with code\n" +
                                "3. Enter $groupCode"
                    )
                }
                ctx.startActivity(Intent.createChooser(share, "Share invite"))
            },
        )
        Spacer(Modifier.height(10.dp))
        RidgeGhostButton("Done", onClick = onBack)
        Spacer(Modifier.height(12.dp))
    }
}
