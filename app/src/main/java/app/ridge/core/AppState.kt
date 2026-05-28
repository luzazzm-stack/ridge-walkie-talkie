package app.ridge.core

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ConnMode { Bluetooth, WifiDirect, Auto }
enum class TalkMode { Hold, HandsFree }
enum class AudioOut { Earpiece, Speaker, BluetoothHeadset, Wired }

@Immutable
data class Member(
    val id: String,
    val name: String,
    val initial: Char,
    val isLead: Boolean = false,
    val rssi: Int = -60,         // dBm
    val distanceM: Int = 0,
    val talking: Boolean = false,
    val lastSeenSec: Int = 0,
    val viaBluetooth: Boolean = false,
)

@Immutable
data class UiState(
    val night: Boolean = false,
    val connMode: ConnMode = ConnMode.WifiDirect,
    val autoMode: Boolean = true,
    val talkMode: TalkMode = TalkMode.Hold,
    val audioOut: AudioOut = AudioOut.Speaker,
    val btHeadsetName: String? = null,
    val btHeadsetBattery: Int? = null,
    val wiredPluggedIn: Boolean = false,
    val transmitting: Boolean = false,
    val voxLevel: Int = 60,
    val members: List<Member> = emptyList(),
    val groupName: String = "",
    val groupCode: String = "4417",
    val sosLive: Boolean = false,
    val sosFromName: String? = null,
    val batteryPct: Int = 81,
    val haptic: Boolean = true,
    val chime: Boolean = true,
    val boost: Boolean = false,
    val btRelay: Boolean = true,
    val saverThreshold: Int = 20,
    val voiceQuality: Float = 0.58f,   // 0..1
    val sosArmed: Boolean = true,
    val needsPermissions: Boolean = true,
    val hasGroup: Boolean = false,
)

class RidgeStore {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setNight(v: Boolean) = _state.update { it.copy(night = v) }
    fun setConnMode(m: ConnMode) = _state.update { it.copy(connMode = m) }
    fun toggleAutoMode() = _state.update { it.copy(autoMode = !it.autoMode) }
    fun setTalkMode(m: TalkMode) = _state.update { it.copy(talkMode = m) }
    fun setAudioOut(a: AudioOut) = _state.update { it.copy(audioOut = a) }
    fun setBtHeadset(name: String?, batt: Int?) =
        _state.update { it.copy(btHeadsetName = name, btHeadsetBattery = batt) }
    fun setTransmitting(v: Boolean) = _state.update { it.copy(transmitting = v) }
    fun setVoxLevel(v: Int) = _state.update { it.copy(voxLevel = v.coerceIn(0, 100)) }

    fun fireSos(from: String) = _state.update { it.copy(sosLive = true, sosFromName = from) }
    fun clearSos() = _state.update { it.copy(sosLive = false, sosFromName = null) }

    fun setHaptic(v: Boolean) = _state.update { it.copy(haptic = v) }
    fun setChime(v: Boolean) = _state.update { it.copy(chime = v) }
    fun setBoost(v: Boolean) = _state.update { it.copy(boost = v) }
    fun setBtRelay(v: Boolean) = _state.update { it.copy(btRelay = v) }
    fun setSaverThreshold(v: Int) = _state.update { it.copy(saverThreshold = v) }
    fun setVoiceQuality(v: Float) = _state.update { it.copy(voiceQuality = v.coerceIn(0f, 1f)) }
    fun setSosArmed(v: Boolean) = _state.update { it.copy(sosArmed = v) }

    fun setNeedsPermissions(v: Boolean) = _state.update { it.copy(needsPermissions = v) }
    fun setHasGroup(v: Boolean) = _state.update { it.copy(hasGroup = v) }
    fun setGroupName(name: String) = _state.update { it.copy(groupName = name) }
    fun setGroupCode(code: String) = _state.update { it.copy(groupCode = code) }

    fun startGroup(name: String) = _state.update {
        it.copy(
            hasGroup = true,
            groupName = name.trim().ifBlank { "Untitled group" },
            groupCode = (1000..9999).random().toString(),
            members = emptyList(),
            transmitting = false,
            sosLive = false,
        )
    }

    fun leaveGroup() = _state.update {
        it.copy(
            hasGroup = false,
            groupName = "",
            members = emptyList(),
            transmitting = false,
            sosLive = false,
        )
    }

    fun updateMembers(transform: (List<Member>) -> List<Member>) =
        _state.update { it.copy(members = transform(it.members)) }

    companion object {
        val Instance by lazy { RidgeStore() }
    }
}

