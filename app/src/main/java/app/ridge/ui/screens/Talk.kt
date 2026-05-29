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
import app.ridge.core.HotspotState
import app.ridge.core.JoinState
import app.ridge.core.Member
import app.ridge.core.Role
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
        // real connection status (replaces old BT vs Wi-Fi Direct strip)
        ConnStatusBar(state = state)

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
                        .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
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
                        .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
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
                        .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
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

            // members — derived from the real transport peer count
            val onlineCount = if (state.transportActive) state.peerCount else 0
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 11.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RidgeLabel("Members · ${onlineCount + 1}")  // +1 = you
                Spacer(Modifier.weight(1f))
                RidgeLabel(if (state.transmitting) "● ON AIR" else "LISTENING", color = c.hivisInk)
            }

            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // "You" row — always present
                MemberSimpleRow(
                    name = "You",
                    sub = if (state.role == Role.Host) "host of ${state.groupName.ifBlank { "this group" }}" else "joined",
                    talking = state.transmitting,
                    isYou = true,
                )
                // connected peers
                if (onlineCount > 0) {
                    val peerLabel = if (state.role == Role.Host) "Member" else "Host"
                    repeat(onlineCount) { i ->
                        MemberSimpleRow(
                            name = if (onlineCount == 1 && state.role != Role.Host) "Host"
                                   else "$peerLabel ${i + 1}",
                            sub = "connected",
                            talking = false,
                            isYou = false,
                        )
                    }
                } else if (!state.transportActive) {
                    RidgeCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Waiting to connect…",
                                fontFamily = RidgeTheme.type.display,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = c.ink,
                            )
                            Text(
                                if (state.role == Role.Host)
                                    "Tap + INVITE so friends can join. They appear here when connected."
                                else "Finishing the connection to the host…",
                                fontFamily = RidgeTheme.type.mono,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = c.muted,
                            )
                        }
                    }
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
                        if (state.transportActive) "v0.10 · voice link live" else "v0.10 · waiting to connect",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        color = c.hivisInk,
                    )
                    Text(
                        if (state.transportActive) {
                            "UDP voice live. ${state.peerCount} peer${if (state.peerCount == 1) "" else "s"}. Press PTT to talk, or switch to Hands-free."
                        } else if (state.role == Role.Host) {
                            "Tap + INVITE to show your QR + code so friends can join. The status bar at top says when peers connect."
                        } else {
                            "Waiting for the connection to your group's host. Status bar at top shows progress."
                        },
                        fontFamily = RidgeTheme.type.mono,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = c.ink,
                    )
                    // packet counters — proves bytes are flowing even if audio is silent
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "sent ${state.txPackets} · recv ${state.rxPackets} packets" +
                            if (state.lastError.isNotBlank()) "  ·  ⚠ ${state.lastError}" else "",
                        fontFamily = RidgeTheme.type.mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = if (state.lastError.isNotBlank()) c.alarm else c.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnStatusBar(state: UiState) {
    val c = RidgeTheme.colors
    val role = state.role
    val ts = state.transportActive
    val (dotColor, label, sub) = when {
        ts && role == Role.Host -> Triple(
            c.field,
            "HOSTING",
            "${state.peerCount} connected · code ${state.groupCode}"
        )
        ts && role == Role.Joiner -> Triple(
            c.field,
            "CONNECTED",
            "joined · code ${state.groupCode}"
        )
        role == Role.Host && state.hotspotState is HotspotState.Active -> Triple(
            c.beacon,
            "WAITING FOR PEERS",
            "hotspot \"${(state.hotspotState as HotspotState.Active).ssid}\" · 0 connected"
        )
        role == Role.Joiner && state.joinState is JoinState.Connecting -> Triple(
            c.beacon,
            "CONNECTING",
            "joining ${(state.joinState as JoinState.Connecting).ssid}…"
        )
        role == Role.Joiner && state.joinState is JoinState.Failed -> Triple(
            c.alarm,
            "CONNECT FAILED",
            (state.joinState as JoinState.Failed).reason.take(40)
        )
        role == Role.Host -> Triple(
            c.beacon,
            "HOST · NOT READY",
            "open Invite to set up hotspot"
        )
        else -> Triple(c.muted, "IDLE", "no group")
    }

    Box(Modifier.fillMaxWidth().background(c.ink).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.0.sp,
                    color = c.bone,
                )
                Text(
                    sub,
                    fontFamily = RidgeTheme.type.mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = c.bone.copy(alpha = 0.75f),
                )
            }
        }
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
            .border(BorderStroke(2.5.dp, c.border), RoundedCornerShape(7.dp))
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
private fun MemberSimpleRow(name: String, sub: String, talking: Boolean, isYou: Boolean) {
    val c = RidgeTheme.colors
    RidgeCard(
        Modifier.fillMaxWidth(),
        border = if (talking) c.hivis else c.border,
    ) {
        Row(
            Modifier.padding(start = 11.dp, end = 11.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Avatar(
                letter = name.firstOrNull()?.uppercaseChar() ?: '?',
                bg = if (talking) c.beacon else if (isYou) c.hivis else Color(0xFFCFE3DC),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontFamily = RidgeTheme.type.display,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = c.ink,
                )
                Text(
                    if (talking) "talking now" else sub,
                    fontFamily = RidgeTheme.type.mono,
                    fontSize = 10.5.sp,
                    color = if (talking) c.hivisInk else c.muted,
                )
            }
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .background(if (talking) c.hivis else c.field)
            )
        }
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
                .border(BorderStroke(s.borderHeavy, c.border), CircleShape),
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
