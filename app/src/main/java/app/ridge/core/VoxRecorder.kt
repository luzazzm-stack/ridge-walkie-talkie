package app.ridge.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Continuously samples the mic on a background thread and reports a 0..1
 * RMS level + an activity boolean (level > threshold).
 *
 * Used by [RadioService] to drive Hands-free / VOX detection.
 * Lives entirely on the device — audio bytes are not stored or transmitted
 * in this milestone. M5 will wire encode + UDP send here.
 */
class VoxRecorder(
    private val ctx: Context,
    private val onLevel: (Float) -> Unit,
    private val onActiveChanged: (Boolean) -> Unit,
) {
    private val running = AtomicBoolean(false)
    private var recorder: AudioRecord? = null
    private var thread: Thread? = null

    /** Returns true if mic was started, false if perm missing or device denied it. */
    @SuppressLint("MissingPermission")
    fun start(threshold: Float = 0.06f): Boolean {
        if (running.get()) return true
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return false

        val sampleRate = 16_000
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        if (minBuf <= 0) return false

        val r = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, channel, encoding, minBuf * 2)
        } catch (_: Throwable) { return false }

        if (r.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { r.release() }
            return false
        }

        return try {
            r.startRecording()
            recorder = r
            running.set(true)
            thread = Thread({ loop(minBuf, threshold) }, "ridge-vox").apply {
                isDaemon = true
                start()
            }
            true
        } catch (_: Throwable) {
            runCatching { r.release() }
            recorder = null
            false
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        thread?.interrupt()
        thread = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        onActiveChanged(false)
        onLevel(0f)
    }

    private fun loop(bufSize: Int, threshold: Float) {
        val buf = ShortArray(bufSize)
        var lastActive = false
        // small hysteresis to avoid flicker
        val onThreshold = threshold
        val offThreshold = threshold * 0.6f
        // require N consecutive frames over threshold before flipping
        var aboveCount = 0
        var belowCount = 0
        val flipFrames = 3

        while (running.get()) {
            val rec = recorder ?: break
            val n = try { rec.read(buf, 0, buf.size) } catch (_: Throwable) { -1 }
            if (n <= 0) {
                Thread.sleep(20)
                continue
            }
            var sumSq = 0.0
            for (i in 0 until n) {
                val v = buf[i].toDouble()
                sumSq += v * v
            }
            val rms = sqrt(sumSq / n) / Short.MAX_VALUE.toDouble()
            val level = rms.coerceIn(0.0, 1.0).toFloat()
            onLevel(level)

            val limit = if (lastActive) offThreshold else onThreshold
            if (level >= limit) {
                aboveCount++; belowCount = 0
            } else {
                belowCount++; aboveCount = 0
            }
            if (!lastActive && aboveCount >= flipFrames) {
                lastActive = true
                onActiveChanged(true)
            } else if (lastActive && belowCount >= flipFrames * 4) {
                lastActive = false
                onActiveChanged(false)
            }
        }
    }
}
