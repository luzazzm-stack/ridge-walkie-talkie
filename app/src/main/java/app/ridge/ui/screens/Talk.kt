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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ridge.core.AudioOut
import app.ridge.core.ConnMode
import app.ridge.core.Member
import app.ridge.core.TalkMode
import app.ridge.core.UiState
import app.ridge.ui.components.Avatar
import app.ridge.ui.components.Divider2
import app.ridge.ui.components.PttButton
import app.ridge.ui.components.RidgeCard
import app.ridge.ui.components.RidgeChip
import app.ridge.ui.components.RidgeLabel
import app.ridge.ui.components.Segmented
import app.ridge.ui.components.SignalBars
import app.ridge.ui.components.WaveBars
import app.ridge.ui.theme.RidgeTheme

@Composable
fun TalkScreen(
    state: UiState,
    onConnMode: (ConnMode) -> Unit,
    onTalkMode: (TalkMode) -> Unit,
    onPress: (Boolean) -> Unit,
    onOpenAudio: () -> Unit,
    onSettings: () -> Unit,
    onInvite: () -> Unit,
    onLeave: () -> Unit,
    onTestSos: () -> Unit,
) {
    val c = RidgeTheme.colors

    Column(Modifier.fillMaxSize().background(c.bone)) {
        // connection mode strip
        ConnModeStrip(state.connMode, state.autoMode, onSelect = onConnMode)

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // channel header — back + channel + invite + settings
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(c.paper)
                        .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(7.dp))
                        .clickable(onClick = onLeave),
                    contentAlignment = Alignment.Center,
                ) { Text("‹", fontSize = 20.sp, fontWeight = FontWeight.Black, color = c.ink) }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f).clickable(onClick = onTestSos)) {
                    RidgeLabel("Channel", color = c.muted)
                    Text(
                        state.groupName.ifBlank { "Untitled" },
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = (-0.4).sp,
                        color = c.ink,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(c.hivis)
                        .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(7.dp))
                        .clickable(onClick = onInvite)
                        .padding(horizontal = 11.dp, vertical = 8.dp),
                ) {
                    Text(
                        "+ INVITE",
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(c.paper)
                        .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(7.dp))
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) { Text("⚙", fontSize = 18.sp, color = c.ink) }
            }

            // second row: audio output pill
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                AudioOutChip(state, onClick = onOpenAudio)
            }

            // PTT or VOX
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                if (state.talkMode == TalkMode.Hold) {
                    PttButton(talking = state.transmitting, onPressChange = onPress)
                } else {
                    VoxListenRing(active = true)
                }
            }

            Column(Modifier.padding(horizontal = 18.dp)) {
                Segmented(
                    options = listOf("● Hold", "○ Hands-free").let {
                        if (state.talkMode == TalkMode.Hold) listOf("● Hold", "○ Hands-free")
                        else listOf("○ Hold", "● Hands-free")
                    },
                    selectedIndex = if (state.talkMode == TalkMode.Hold) 0 else 1,
                    onSelect = { onTalkMode(if (it == 0) TalkMode.Hold else TalkMode.HandsFree) },
                )
            }
            Spacer(Modifier.height(12.dp))
            Divider2(modifier = Modifier.padding(horizontal = 18.dp))

            // members
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 11.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RidgeLabel("Members · ${state.members.size}")
                Spacer(Modifier.weight(1f))
                RidgeLabel("↕ DISTANCE", color = c.hivisInk)
            }

            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.members.isEmpty()) {
                    RidgeCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Waiting for group members…",
                                fontFamily = RidgeTheme.type.display,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = c.ink,
                            )
                            Text(
                                "Share code ${state.groupCode.ifBlank { "—" }} with your group, or have them scan your QR. Anyone within ~80m will appear here.",
                                fontFamily = RidgeTheme.type.mono,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = c.muted,
                            )
                        }
                    }
                } else {
                    state.members.forEach { MemberRow(it) }
                }
            }

            // Honesty banner — voice transport isn't wired yet
            Box(
                Modifier
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(c.beacon.copy(alpha = 0.18f))
                    .border(BorderStroke(2.5.dp, c.beacon), RoundedCornerShape(7.dp))
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        "v0.2 · UI preview",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = c.hivisInk,
                    )
                    Text(
                        "Discovery is live (BT + Wi-Fi). Voice transport between phones ships in v0.3 — pressing PTT shows the UI state but won't broadcast audio yet.",
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = c.ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnModeStrip(active: ConnMode, auto: Boolean, onSelect: (ConnMode) -> Unit) {
    val c = RidgeTheme.colors
    Box(Modifier.fillMaxWidth().background(c.ink)) {
        Row(Modifier.fillMaxWidth()) {
            ModeButton(
                label = "Bluetooth",
                desc = "~30m · 18 hr",
                active = active == ConnMode.Bluetooth,
                onClick = { onSelect(ConnMode.Bluetooth) },
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(2.5.dp).height(56.dp).background(Color(0xFF2A2A28)))
            ModeButton(
                label = "Wi-Fi Direct",
                desc = "~100m · 2 hr",
                active = active == ConnMode.WifiDirect,
                onClick = { onSelect(ConnMode.WifiDirect) },
                modifier = Modifier.weight(1f),
            )
        }
        if (auto) {
            Box(
                Modifier
                    .padding(top = 6.dp, end = 8.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.paper)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    "AUTO",
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = c.ink,
                )
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    desc: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = RidgeTheme.colors
    Column(
        modifier
            .background(if (active) c.hivis else Color.Transparent)
            .clickable { onClick() }
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(c.bone))
            Text(
                label.uppercase(),
                fontFamily = RidgeTheme.type.display,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.0.sp,
                color = c.bone,
            )
        }
        Text(
            desc,
            fontFamily = RidgeTheme.type.mono,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = c.bone.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun AudioOutChip(state: UiState, onClick: () -> Unit) {
    val c = RidgeTheme.colors
    val label = when (state.audioOut) {
        AudioOut.Speaker -> "Speaker"
        AudioOut.Earpiece -> "Earpiece"
        AudioOut.BluetoothHeadset -> "BT" + (state.btHeadsetName?.let { " · ${it.take(8)}" } ?: "")
        AudioOut.Wired -> "Wired"
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(c.paper)
            .border(BorderStroke(2.5.dp, c.ink), RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "🔊",
            fontSize = 12.sp,
        )
        Text(
            label.uppercase(),
            fontFamily = RidgeTheme.type.mono,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.6.sp,
            color = c.ink,
        )
        Text("▼", fontSize = 10.sp, color = c.ink)
    }
}

@Composable
private fun MemberRow(m: Member) {
    val c = RidgeTheme.colors
    val talkingBorderColor = if (m.talking) c.hivis else c.ink
    RidgeCard(
        Modifier.fillMaxWidth(),
        border = talkingBorderColor,
    ) {
        Row(
            Modifier.padding(start = 11.dp, end = 11.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            val avBg = when {
                m.talking -> c.beacon
                m.viaBluetooth -> c.bone
                else -> Color(0xFFCFE3DC)
            }
            Avatar(m.initial, avBg)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        m.name,
                        fontFamily = RidgeTheme.type.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = c.ink,
                    )
                    if (m.isLead) Text(
                        " · LEAD",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = c.field,
                    )
                }
                val sub = when {
                    m.talking -> "talking now"
                    m.viaBluetooth -> "via Bluetooth relay"
                    m.lastSeenSec > 0 -> "idle · ${m.lastSeenSec / 60}m ago"
                    else -> "idle"
                }
                Text(sub, fontFamily = RidgeTheme.type.mono, fontSize = 10.5.sp, color = c.muted)
            }
            if (m.talking) {
                WaveBars(live = true, bars = 6, modifier = Modifier.height(18.dp))
                Spacer(Modifier.size(6.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                val strength = when {
                    m.rssi >= -55 -> 4
                    m.rssi >= -65 -> 3
                    m.rssi >= -75 -> 2
                    m.rssi >= -85 -> 1
                    else -> 1
                }
                val weak = m.rssi < -70
                SignalBars(strength, weak)
                Spacer(Modifier.height(2.dp))
                val distStr = if (m.viaBluetooth) "~${m.distanceM}m" else "${m.distanceM}m"
                Text(
                    if (weak) "$distStr · weak" else distStr,
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (weak) c.hivisInk else c.ink,
                )
            }
        }
    }
}

@Composable
private fun VoxListenRing(active: Boolean) {
    val c = RidgeTheme.colors
    val s = RidgeTheme.shape
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(220.dp).clip(CircleShape).border(BorderStroke(2.5.dp, c.ink.copy(alpha = 0.08f)), CircleShape))
        Box(Modifier.size(196.dp).clip(CircleShape).border(BorderStroke(2.5.dp, c.ink.copy(alpha = 0.16f)), CircleShape))
        // shadow
        Box(Modifier.size(184.dp).offset(y = 6.dp).clip(CircleShape).background(c.ink))
        Box(
            Modifier
                .size(184.dp)
                .clip(CircleShape)
                .background(if (active) c.hivis else c.paper)
                .border(BorderStroke(s.borderHeavy, c.ink), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(c.ink))
                WaveBars(live = active, bars = 10, color = if (active) c.paper else c.ink)
                Text(
                    "LISTENING",
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.3.sp,
                    color = c.paper,
                )
                Text(
                    "AUTO-TRANSMIT ON SPEECH",
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = c.paper.copy(alpha = 0.85f),
                )
            }
        }
    }
}
