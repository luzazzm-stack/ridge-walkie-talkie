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

    fun apply(target: AudioOut) {
        // Pre-S APIs are sufficient for the common phone outputs we care about
        am.mode = AudioManager.MODE_IN_COMMUNICATION
        when (target) {
            AudioOut.Speaker -> {
                am.isSpeakerphoneOn = true
                am.stopBluetoothSco()
                am.isBluetoothScoOn = false
            }
            AudioOut.Earpiece -> {
                am.isSpeakerphoneOn = false
                am.stopBluetoothSco()
                am.isBluetoothScoOn = false
            }
            AudioOut.BluetoothHeadset -> {
                am.isSpeakerphoneOn = false
                am.startBluetoothSco()
                am.isBluetoothScoOn = true
            }
            AudioOut.Wired -> {
                am.isSpeakerphoneOn = false
                am.stopBluetoothSco()
                am.isBluetoothScoOn = false
            }
        }
    }

    fun release() {
        runCatching { am.stopBluetoothSco() }
        am.isBluetoothScoOn = false
        am.isSpeakerphoneOn = false
        am.mode = AudioManager.MODE_NORMAL
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
