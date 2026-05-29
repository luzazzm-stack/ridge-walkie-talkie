package app.ridge.core

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager

class AudioRouter(private val ctx: Context) {

    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Audio plays via an AudioTrack with USAGE_MEDIA (see VoiceTransport), which
     * routes automatically: loudspeaker by default, A2DP when a Bluetooth headset
     * is connected, wired headset when plugged. We therefore DON'T force
     * MODE_IN_COMMUNICATION (that fights the media stream and silenced playback
     * on many OEMs). For a mono Bluetooth headset we optionally start SCO, guarded
     * by the runtime permission. Speaker/Earpiece/Wired need no action here.
     */
    fun apply(target: AudioOut) {
        when (target) {
            AudioOut.BluetoothHeadset -> if (hasBtConnect()) {
                runCatching { am.startBluetoothSco(); am.isBluetoothScoOn = true }
            }
            else -> runCatching {
                if (am.isBluetoothScoOn) { am.stopBluetoothSco(); am.isBluetoothScoOn = false }
            }
        }
    }

    fun release() {
        runCatching { if (am.isBluetoothScoOn) am.stopBluetoothSco() }
        runCatching { am.isBluetoothScoOn = false }
    }

    private fun hasBtConnect(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return true
        return androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun isWiredPluggedIn(): Boolean {
        val devs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devs.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }

    fun connectedBtHeadsetName(): String? {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return null
        val adapter = bm.adapter ?: return null
        return runCatching {
            // Permission-safe: catches SecurityException if BLUETOOTH_CONNECT not granted
            val connected = adapter.getProfileConnectionState(BluetoothProfile.HEADSET)
            if (connected != BluetoothProfile.STATE_CONNECTED) return@runCatching null
            val devs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }?.productName?.toString()
        }.getOrNull()
    }
}
