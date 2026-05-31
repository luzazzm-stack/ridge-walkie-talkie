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
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/**
 * UDP voice transport with a tiny typed-packet protocol.
 *
 * Packet = [1 type byte][payload]:
 *   - TYPE_AUDIO (0): payload is raw 16-bit PCM @ 16 kHz mono, ~20 ms frame
 *   - TYPE_HELLO (1): payload is the sender's UTF-8 display name
 *
 * Host mode: opens UDP server on [PORT]; relays audio between clients and
 * learns each peer's name from their HELLO. Replies to each HELLO with its
 * own HELLO so clients learn the host's name.
 * Client mode: sends HELLO (name) to host every ~1.5 s; sends/plays audio.
 */
class VoiceTransport(
    private val ctx: Context,
    private val onLevel: (Float) -> Unit,
    private val onActiveChanged: (Boolean) -> Unit,
    private val onPeers: (List<String>) -> Unit,
    private val onError: (String) -> Unit,
    private val onStats: (txPackets: Long, rxPackets: Long) -> Unit = { _, _ -> },
    private val onIncomingStart: () -> Unit = {},
    private val onDiag: (myIp: String, targetIp: String) -> Unit = { _, _ -> },
) {

    enum class Mode { Host, Client }

    /** Identity is ip+port only (immutable) so it's safe as a map key. */
    private data class PeerKey(val ip: InetAddress, val port: Int)

    @Volatile private var myName: String = "Me"
    fun setMyName(name: String) { myName = name.ifBlank { "Me" } }

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val running = AtomicBoolean(false)
    private val voxEnabled = AtomicBoolean(false)
    private val holdPressed = AtomicBoolean(false)
    private var sender: Thread? = null
    private var receiver: Thread? = null
    private var statsThread: Thread? = null
    private var keepAliveThread: Thread? = null

    private var socket: DatagramSocket? = null
    private var mode: Mode? = null
    private var hostAddr: InetAddress? = null
    private var hostPort: Int = PORT
    private var hostName: String = "Host"
    private val peers = Collections.synchronizedMap(LinkedHashMap<PeerKey, String>())  // key -> name
    private val peerSeen = Collections.synchronizedMap(HashMap<PeerKey, Long>())       // key -> last packet ms

    private val txPackets = AtomicLong(0)
    private val rxPackets = AtomicLong(0)

    fun startHost(network: Network? = null): Boolean {
        if (running.get()) return true
        mode = Mode.Host
        resetStats()
        val ok = runCatching {
            val s = DatagramSocket(null)
            s.reuseAddress = true
            s.bind(java.net.InetSocketAddress(PORT))   // bind local port FIRST
            runCatching { network?.bindSocket(s) }      // then pin to network (non-fatal)
            socket = s
        }.isSuccess
        if (!ok) { onError("host bind failed on :$PORT"); return false }
        startThreads()
        emitPeers()
        onDiag(localIpv4(), "listening :$PORT")
        return true
    }

    fun startClient(hostIp: String, network: Network?): Boolean {
        if (running.get()) return true
        mode = Mode.Client
        resetStats()
        val ok = runCatching {
            hostAddr = InetAddress.getByName(hostIp)
            hostPort = PORT
            val s = DatagramSocket(null)
            s.reuseAddress = true
            s.bind(null)                                 // bind local ephemeral FIRST
            runCatching { network?.bindSocket(s) }       // then pin to Wi-Fi (non-fatal:
                                                         // on-link route still reaches host)
            socket = s
        }.isSuccess
        if (!ok) { onError("client socket failed"); return false }
        startThreads()
        hostName = "Host"
        emitPeers()
        onDiag(localIpv4(), hostIp)
        return true
    }

    /** Best-effort local site-local IPv4, for on-screen diagnostics. */
    private fun localIpv4(): String = runCatching {
        java.net.NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address &&
                    (it.isSiteLocalAddress || it.hostAddress?.startsWith("192.168.") == true) }
            ?.hostAddress ?: "?"
    }.getOrDefault("?")

    fun stop() {
        running.set(false)
        voxEnabled.set(false)
        holdPressed.set(false)
        sender?.interrupt(); sender = null
        receiver?.interrupt(); receiver = null
        statsThread?.interrupt(); statsThread = null
        keepAliveThread?.interrupt(); keepAliveThread = null
        runCatching { socket?.close() }
        socket = null
        peers.clear()
        peerSeen.clear()
        onPeers(emptyList())
        onActiveChanged(false)
        onLevel(0f)
        _active.value = false
    }

    fun setVoxEnabled(on: Boolean) { voxEnabled.set(on) }
    fun setHoldPressed(on: Boolean) { holdPressed.set(on) }

    private fun resetStats() {
        txPackets.set(0); rxPackets.set(0)
        onStats(0, 0)
    }

    private fun emitPeers() {
        when (mode) {
            Mode.Host -> onPeers(synchronized(peers) { peers.values.toList() })
            Mode.Client -> onPeers(listOf(hostName))
            else -> onPeers(emptyList())
        }
    }

    /** Run a worker body, swallowing interruption (normal stop) and any other
     *  throwable so a background thread can never crash the whole app. */
    private inline fun guarded(crossinline body: () -> Unit): () -> Unit = {
        try { body() } catch (_: InterruptedException) { } catch (_: Throwable) { }
    }

    private fun startThreads() {
        running.set(true)
        _active.value = true
        receiver = Thread(guarded { receiveLoop() }, "ridge-rx").apply { isDaemon = true; start() }
        sender = Thread(guarded { sendLoop() }, "ridge-tx").apply { isDaemon = true; start() }
        statsThread = Thread(guarded { statsLoop() }, "ridge-stats").apply { isDaemon = true; start() }
        if (mode == Mode.Client) {
            keepAliveThread = Thread(guarded { keepAliveLoop() }, "ridge-keepalive").apply { isDaemon = true; start() }
        }
    }

    private fun statsLoop() {
        while (running.get()) {
            onStats(txPackets.get(), rxPackets.get())
            if (mode == Mode.Host) reapStalePeers()
            runCatching { Thread.sleep(1000) }.getOrElse { return }
        }
    }

    /** Drop peers we haven't heard from in a while — removes "ghost" entries
     *  left behind when a device disconnects and rejoins on a new port. */
    private fun reapStalePeers() {
        val now = System.currentTimeMillis()
        val removed = synchronized(peers) {
            val dead = peerSeen.filter { now - it.value > PEER_TIMEOUT_MS }.keys.toList()
            dead.forEach { peers.remove(it); peerSeen.remove(it) }
            dead.isNotEmpty()
        }
        if (removed) emitPeers()
    }

    /** Client announces its name to the host every ~1.5 s so the host registers
     *  it (and re-registers after any drop) even if early hellos were lost. */
    private fun keepAliveLoop() {
        while (running.get()) {
            sendHello()
            runCatching { Thread.sleep(1500) }.getOrElse { return }
        }
    }

    private fun helloPacket(): ByteArray {
        val nameBytes = myName.toByteArray(Charsets.UTF_8).take(80).toByteArray()
        return ByteArray(nameBytes.size + 1).also {
            it[0] = TYPE_HELLO
            System.arraycopy(nameBytes, 0, it, 1, nameBytes.size)
        }
    }

    private fun sendHello() {
        val s = socket ?: return
        val pkt = helloPacket()
        when (mode) {
            Mode.Client -> {
                val addr = hostAddr ?: return
                runCatching { s.send(DatagramPacket(pkt, pkt.size, addr, hostPort)) }
            }
            Mode.Host -> {
                for (k in peerKeys()) {
                    runCatching { s.send(DatagramPacket(pkt, pkt.size, k.ip, k.port)) }
                }
            }
            else -> Unit
        }
    }

    private fun peerKeys(): List<PeerKey> = synchronized(peers) { peers.keys.toList() }

    private fun receiveLoop() {
        val s = socket ?: return
        val buf = ByteArray(MAX_PACKET)
        val track = newTrack()
        var lastAudioMs = 0L
        try {
            track.play()
            while (running.get()) {
                val pkt = DatagramPacket(buf, buf.size)
                runCatching { s.receive(pkt) }.getOrNull() ?: continue
                rxPackets.incrementAndGet()
                val len = pkt.length
                if (len < 1) continue
                val type = buf[0]
                val src = pkt.address ?: continue
                if (mode == Mode.Host) peerSeen[PeerKey(src, pkt.port)] = System.currentTimeMillis()

                when (type) {
                    TYPE_HELLO -> {
                        val name = if (len > 1) String(buf, 1, len - 1, Charsets.UTF_8).trim() else ""
                        if (mode == Mode.Host) {
                            val key = PeerKey(src, pkt.port)
                            val isNew = synchronized(peers) { !peers.containsKey(key) }
                            synchronized(peers) {
                                // add new, or update a non-blank name; never blank out a known name
                                if (isNew || (name.isNotBlank())) peers[key] = name.ifBlank { peers[key] ?: "Member" }
                            }
                            if (isNew) {
                                // greet back so the client learns the host's name
                                val h = helloPacket()
                                runCatching { s.send(DatagramPacket(h, h.size, src, pkt.port)) }
                            }
                            emitPeers()
                        } else if (mode == Mode.Client) {
                            if (name.isNotBlank() && name != hostName) {
                                hostName = name
                                emitPeers()
                            }
                        }
                    }
                    TYPE_AUDIO -> {
                        if (mode == Mode.Host) {
                            // relay to all OTHER peers (preserve type byte)
                            val out = pkt.data.copyOfRange(0, len)
                            for (k in peerKeys()) {
                                if (k.ip != src || k.port != pkt.port) runCatching {
                                    s.send(DatagramPacket(out, out.size, k.ip, k.port))
                                    txPackets.incrementAndGet()
                                }
                            }
                        }
                        // play payload (skip the type byte)
                        if (len > 1) {
                            // chime hook: a fresh transmission after >800 ms gap
                            val now = System.currentTimeMillis()
                            if (now - lastAudioMs > 800) runCatching { onIncomingStart() }
                            lastAudioMs = now
                            runCatching { track.write(buf, 1, len - 1) }
                        }
                    }
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
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
        } catch (t: Throwable) {
            onError("AudioRecord ctor: ${t.message}"); return
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release(); onError("AudioRecord uninitialized"); return
        }
        val aec = runCatching {
            if (AcousticEchoCanceler.isAvailable())
                AcousticEchoCanceler.create(rec.audioSessionId)?.apply { enabled = true } else null
        }.getOrNull()
        val ns = runCatching {
            if (NoiseSuppressor.isAvailable())
                NoiseSuppressor.create(rec.audioSessionId)?.apply { enabled = true } else null
        }.getOrNull()
        val pcm = ByteArray(FRAME_BYTES)
        val out = ByteArray(FRAME_BYTES + 1)  // [type][pcm]
        out[0] = TYPE_AUDIO
        var lastActive = false
        var aboveCount = 0
        var belowCount = 0
        try {
            rec.startRecording()
            while (running.get()) {
                val n = rec.read(pcm, 0, pcm.size)
                if (n <= 0) { Thread.sleep(10); continue }

                val rms = computeRms(pcm, n)
                onLevel(rms)

                val voxActive = if (voxEnabled.get()) {
                    val limit = if (lastActive) VOX_OFF else VOX_ON
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
                if (nowActive) {
                    System.arraycopy(pcm, 0, out, 1, n)
                    sendAudio(out, n + 1)
                }
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { aec?.release() }
            runCatching { ns?.release() }
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

    private fun sendAudio(data: ByteArray, len: Int) {
        val s = socket ?: return
        val out = if (len == data.size) data else data.copyOf(len)
        when (mode) {
            Mode.Host -> {
                for (k in peerKeys()) {
                    runCatching {
                        s.send(DatagramPacket(out, out.size, k.ip, k.port))
                        txPackets.incrementAndGet()
                    }
                }
            }
            Mode.Client -> {
                val addr = hostAddr ?: return
                runCatching {
                    s.send(DatagramPacket(out, out.size, addr, hostPort))
                    txPackets.incrementAndGet()
                }
            }
            else -> Unit
        }
    }

    private fun newTrack(): AudioTrack {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val fmt = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        // Larger buffer (~bigger jitter cushion) → fewer underruns → clearer, less choppy audio.
        val bufBytes = (minBuf * 8).coerceAtLeast(SAMPLE_RATE / 2)  // >= ~0.25s
        return AudioTrack(attrs, fmt, bufBytes, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE).apply {
            runCatching { setVolume(AudioTrack.getMaxVolume()) }
        }
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
        const val TYPE_AUDIO: Byte = 0
        const val TYPE_HELLO: Byte = 1
        const val PEER_TIMEOUT_MS = 6000L   // ~4 missed 1.5s keepalives → drop ghost
    }
}
