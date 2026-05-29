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
import android.net.Network
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * UDP voice transport.
 *
 *  - Host mode: opens UDP server on [PORT]; relays packets between clients.
 *  - Client mode: sends/receives to the host IP on the joined Wi-Fi network.
 *
 * One AudioRecord opens per session. PTT-Hold or hands-free VOX both drive
 * "should we send right now?" through this same mic, so we never conflict.
 *
 * Audio: raw 16-bit signed PCM @ 16 kHz mono, ~20ms frames. No codec yet —
 * verify the link first, optimize bandwidth in a later milestone.
 */
class VoiceTransport(
    private val ctx: Context,
    private val onLevel: (Float) -> Unit,
    private val onActiveChanged: (Boolean) -> Unit,
    private val onPeerCount: (Int) -> Unit,
    private val onError: (String) -> Unit,
) {

    enum class Mode { Host, Client }

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val running = AtomicBoolean(false)
    private val voxEnabled = AtomicBoolean(false)   // true → send when RMS > threshold
    private val holdPressed = AtomicBoolean(false)  // true → send while pressed
    private var sender: Thread? = null
    private var receiver: Thread? = null

    private var socket: DatagramSocket? = null
    private var mode: Mode? = null
    private var hostAddr: InetAddress? = null
    private val peers = Collections.synchronizedSet(mutableSetOf<InetAddress>())

    fun startHost(network: Network? = null) {
        if (running.get()) return
        mode = Mode.Host
        runCatching {
            val s = DatagramSocket(PORT)
            network?.bindSocket(s)
            socket = s
        }.onFailure { onError("host bind: ${it.message}"); return }
        startThreads()
    }

    fun startClient(hostIp: String, network: Network?) {
        if (running.get()) return
        mode = Mode.Client
        runCatching {
            hostAddr = InetAddress.getByName(hostIp)
            val s = DatagramSocket()
            network?.bindSocket(s)
            socket = s
            // hello packet so host knows we're here
            val hello = byteArrayOf(0)
            s.send(DatagramPacket(hello, hello.size, hostAddr, PORT))
        }.onFailure { onError("client connect: ${it.message}"); return }
        startThreads()
    }

    fun stop() {
        running.set(false)
        voxEnabled.set(false)
        holdPressed.set(false)
        sender?.interrupt(); sender = null
        receiver?.interrupt(); receiver = null
        runCatching { socket?.close() }
        socket = null
        peers.clear()
        onPeerCount(0)
        onActiveChanged(false)
        onLevel(0f)
        _active.value = false
    }

    /** Hands-free / VOX: keep mic open, send when RMS over threshold. */
    fun setVoxEnabled(on: Boolean) { voxEnabled.set(on) }

    /** PTT Hold: caller flips on press / off on release. */
    fun setHoldPressed(on: Boolean) { holdPressed.set(on) }

    private fun startThreads() {
        running.set(true)
        _active.value = true
        receiver = Thread({ receiveLoop() }, "ridge-rx").apply { isDaemon = true; start() }
        sender = Thread({ sendLoop() }, "ridge-tx").apply { isDaemon = true; start() }
    }

    private fun receiveLoop() {
        val s = socket ?: return
        val buf = ByteArray(MAX_PACKET)
        val track = newTrack()
        try {
            track.play()
            while (running.get()) {
                val pkt = DatagramPacket(buf, buf.size)
                runCatching { s.receive(pkt) }.getOrNull() ?: continue
                val src = pkt.address
                if (src != null && mode == Mode.Host) {
                    if (peers.add(src)) onPeerCount(peers.size)
                    // relay to other peers
                    val out = pkt.data.copyOfRange(0, pkt.length)
                    for (p in peers.toSet()) {
                        if (p != src) runCatching {
                            s.send(DatagramPacket(out, out.size, p, PORT))
                        }
                    }
                }
                // play locally (skip the 1-byte hello)
                if (pkt.length > 1) {
                    runCatching { track.write(pkt.data, 0, pkt.length) }
                }
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendLoop() {
        if (!hasMic()) { onError("mic permission missing"); return }
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) { onError("AudioRecord minBuf"); return }
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
        } catch (t: Throwable) {
            onError("AudioRecord ctor: ${t.message}"); return
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release(); onError("AudioRecord uninitialized"); return
        }
        val buf = ByteArray(FRAME_BYTES)
        var lastActive = false
        var aboveCount = 0
        var belowCount = 0
        val onThresh = VOX_ON
        val offThresh = VOX_OFF
        try {
            rec.startRecording()
            while (running.get()) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) { Thread.sleep(10); continue }

                // RMS for level meter
                val rms = computeRms(buf, n)
                onLevel(rms)

                // decide: PTT hold or VOX
                val voxActive = if (voxEnabled.get()) {
                    val limit = if (lastActive) offThresh else onThresh
                    if (rms >= limit) { aboveCount++; belowCount = 0 } else { belowCount++; aboveCount = 0 }
                    when {
                        !lastActive && aboveCount >= 3 -> true
                        lastActive && belowCount >= 12 -> false
                        else -> lastActive
                    }
                } else false
                val nowActive = voxActive || holdPressed.get()

                if (nowActive != lastActive) {
                    lastActive = nowActive
                    onActiveChanged(nowActive)
                }
                if (nowActive) sendBytes(buf, n)
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { rec.stop() }
            runCatching { rec.release() }
        }
    }

    private fun computeRms(buf: ByteArray, n: Int): Float {
        var sumSq = 0.0
        var i = 0
        while (i + 1 < n) {
            val v = (buf[i].toInt() and 0xFF) or (buf[i + 1].toInt() shl 8)
            val s = if (v > 32767) v - 65536 else v
            sumSq += (s * s).toDouble()
            i += 2
        }
        val samples = (n / 2).coerceAtLeast(1)
        return (sqrt(sumSq / samples) / Short.MAX_VALUE.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    private fun sendBytes(data: ByteArray, len: Int) {
        val s = socket ?: return
        val out = if (len == data.size) data else data.copyOf(len)
        when (mode) {
            Mode.Host -> {
                for (p in peers.toSet()) {
                    runCatching { s.send(DatagramPacket(out, out.size, p, PORT)) }
                }
            }
            Mode.Client -> {
                val addr = hostAddr ?: return
                runCatching { s.send(DatagramPacket(out, out.size, addr, PORT)) }
            }
            else -> Unit
        }
    }

    private fun newTrack(): AudioTrack {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val fmt = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack(attrs, fmt, minBuf * 4, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    }

    private fun hasMic(): Boolean = ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val PORT = 5050
        const val SAMPLE_RATE = 16_000
        const val FRAME_MS = 20
        const val FRAME_BYTES = SAMPLE_RATE / 1000 * FRAME_MS * 2
        const val MAX_PACKET = 1500
        const val VOX_ON = 0.07f
        const val VOX_OFF = 0.04f
    }
}
