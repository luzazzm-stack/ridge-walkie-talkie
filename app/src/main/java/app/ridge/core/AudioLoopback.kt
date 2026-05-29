package app.ridge.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single-device self-test: records from the mic and plays it straight back to
 * the speaker. Proves the whole capture → playback audio path works WITHOUT any
 * networking or a second phone. If you hear your own voice (slightly delayed),
 * the audio path is fine and any "no sound" problem is the network/peer side.
 */
class AudioLoopback(
    private val ctx: Context,
    private val onLevel: (Float) -> Unit = {},
    private val onError: (String) -> Unit = {},
) {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    fun isRunning() = running.get()

    @SuppressLint("MissingPermission")
    fun start(durationMs: Long = 8000, onDone: () -> Unit = {}) {
        if (running.get()) return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) { onError("Microphone permission not granted"); onDone(); return }

        running.set(true)
        thread = Thread({ loop(durationMs); onDone() }, "ridge-loopback").apply {
            isDaemon = true; start()
        }
    }

    fun stop() {
        running.set(false)
        thread?.interrupt(); thread = null
    }

    @SuppressLint("MissingPermission")
    private fun loop(durationMs: Long) {
        val sr = 16_000
        val minIn = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val minOut = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minIn <= 0 || minOut <= 0) { onError("audio buffer size error"); running.set(false); return }

        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minIn * 2)
        } catch (t: Throwable) { onError("mic open: ${t.message}"); running.set(false); return }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release(); onError("mic uninitialized"); running.set(false); return
        }

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            AudioFormat.Builder().setSampleRate(sr)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
            minOut * 4, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
        ).apply { runCatching { setVolume(AudioTrack.getMaxVolume()) } }

        val buf = ByteArray(640)
        val deadline = System.currentTimeMillis() + durationMs
        try {
            rec.startRecording()
            track.play()
            while (running.get() && System.currentTimeMillis() < deadline) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    onLevel(rms(buf, n))
                    track.write(buf, 0, n)
                }
            }
        } catch (t: Throwable) {
            onError("loopback: ${t.message}")
        } finally {
            runCatching { rec.stop() }; runCatching { rec.release() }
            runCatching { track.stop() }; runCatching { track.release() }
            running.set(false)
            onLevel(0f)
        }
    }

    private fun rms(buf: ByteArray, n: Int): Float {
        var sum = 0.0; var i = 0
        while (i + 1 < n) {
            val v = (buf[i].toInt() and 0xFF) or (buf[i + 1].toInt() shl 8)
            val s = if (v > 32767) v - 65536 else v
            sum += (s * s).toDouble(); i += 2
        }
        val samples = (n / 2).coerceAtLeast(1)
        return (kotlin.math.sqrt(sum / samples) / Short.MAX_VALUE.toDouble()).toFloat().coerceIn(0f, 1f)
    }
}
