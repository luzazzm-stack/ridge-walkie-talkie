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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeGhostButton
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.RidgePrimaryButton
import app.ridge.ui.theme.RidgeTheme

@Composable
fun NameGroupScreen(
    onStart: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = RidgeTheme.colors
    var name by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(c.bone).padding(horizontal = 22.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                    .background(c.paper)
                    .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(8.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.ink) }
            Text(
                "Start a group",
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = c.ink,
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "Name your channel",
            fontFamily = RidgeTheme.type.display,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = (-0.5).sp,
            color = c.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick something your group will recognize.\nExamples: \"Annapurna Base\", \"Trek 3 — Sajan team\".",
            fontFamily = RidgeTheme.type.mono,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = c.muted,
        )
        Spacer(Modifier.height(18.dp))

        RidgeLabel("Channel name")
        Spacer(Modifier.height(8.dp))
        RidgeCard(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= 32) name = it },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = c.ink,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) onStart(name.trim())
                }),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.hivis),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp),
                decorationBox = { inner ->
                    if (name.isEmpty()) {
                        Text(
                            "e.g. Annapurna Base",
                            fontFamily = RidgeTheme.type.display,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = c.muted.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${name.length} / 32",
            fontFamily = RidgeTheme.type.mono,
            fontSize = 10.5.sp,
            color = c.muted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (name.isNotBlank()) {
                RidgePrimaryButton("Start group", onClick = { onStart(name.trim()) })
            } else {
                Box(
                    Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(7.dp))
                        .background(c.bone)
                        .border(BorderStroke(2.5.dp, c.muted), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "ENTER A NAME",
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = c.muted,
                    )
                }
            }
            RidgeGhostButton("Cancel", onClick = onBack)
        }
        Spacer(Modifier.height(6.dp))
    }
}

