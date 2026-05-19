package com.privacyshield.remote.transport

import com.privacyshield.remote.model.RemoteCommand
import com.privacyshield.remote.model.RemoteDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * Transport for Android TV / Google TV devices using the Android TV Remote Protocol v2.
 *
 * Protocol overview:
 *  - Device advertises _androidtvremote2._tcp. via mDNS
 *  - Connect via TLS (device uses a self-signed certificate)
 *  - Exchange framed protobuf messages: [4-byte big-endian length][protobuf bytes]
 *  - First message: RemoteMessage { remote_configure { code1: 622 } }
 *  - Then: RemoteMessage { remote_set_active { active: 1 } }
 *  - Commands: RemoteMessage { remote_key_inject { key_code: N, direction: SHORT } }
 *  - Keepalive: respond to RemotePingRequest with RemotePingResponse
 */
class AndroidTvTransport : RemoteTransport {

    private var sslSocket: SSLSocket? = null
    private var out: DataOutputStream? = null
    private var inp: DataInputStream? = null
    private var readerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val isConnected: Boolean
        get() = sslSocket?.let { !it.isClosed && it.isConnected } ?: false

    override suspend fun connect(device: RemoteDevice): ConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()

                val sslCtx = SSLContext.getInstance("TLS")
                sslCtx.init(null, arrayOf<X509TrustManager>(TrustAllManager()), SecureRandom())

                val socket = sslCtx.socketFactory.createSocket(device.host, device.port) as SSLSocket
                socket.soTimeout = 10_000
                socket.useClientMode = true
                socket.startHandshake()

                sslSocket = socket
                out = DataOutputStream(socket.outputStream.buffered())
                inp = DataInputStream(socket.inputStream.buffered())

                // Step 1: Send configure
                writeMessage(out!!, buildConfigureMessage())
                out!!.flush()

                // Step 2: Read configure response
                val configResp = readMessage(inp!!)

                // Check if pairing is required (field 30 range in the response)
                if (isPairingRequired(configResp)) {
                    return@withContext ConnectionResult.PairingRequired(
                        "Open your Android TV and confirm the pairing request on screen."
                    )
                }

                // Step 3: Send set-active
                writeMessage(out!!, buildSetActiveMessage())
                out!!.flush()

                // Start keepalive / message reader
                startReaderLoop()

                ConnectionResult.Connected
            } catch (e: Exception) {
                cleanupSocket()
                ConnectionResult.Error(e.message ?: "Connection failed")
            }
        }
    }

    override suspend fun sendCommand(command: RemoteCommand): CommandResult {
        return withContext(Dispatchers.IO) {
            val output = out ?: return@withContext CommandResult.Error("Not connected")
            try {
                writeMessage(output, buildKeyInjectMessage(command.keyCode, DIR_SHORT))
                output.flush()
                CommandResult.Sent
            } catch (e: Exception) {
                CommandResult.Error(e.message ?: "Send failed")
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        readerJob?.cancel()
        readerJob = null
        cleanupSocket()
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun startReaderLoop() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val input = inp ?: return@launch
            while (isActive && isConnected) {
                try {
                    val bytes = readMessage(input)
                    handleIncoming(bytes)
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun handleIncoming(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        val tag = bytes[0].toInt() and 0xFF
        when (tag) {
            TAG_PING_REQUEST -> {
                // Parse val1 from RemotePingRequest, send RemotePingResponse
                val val1 = parseVarintAt(bytes, 2) // skip field 11 tag + inner field 1 tag
                val output = out ?: return
                try {
                    writeMessage(output, buildPingResponse(val1))
                    output.flush()
                } catch (_: Exception) {}
            }
        }
    }

    private fun isPairingRequired(bytes: ByteArray): Boolean {
        // If the response has no content or starts with a pairing-range tag (>= 0x72), assume pairing needed
        if (bytes.isEmpty()) return false
        // A valid configure response starts with 0x0A (field 1) and is > 2 bytes
        return bytes[0].toInt() and 0xFF !in listOf(0x0A, 0x32)
    }

    private fun cleanupSocket() {
        try { out?.close() } catch (_: Exception) {}
        try { inp?.close() } catch (_: Exception) {}
        try { sslSocket?.close() } catch (_: Exception) {}
        out = null
        inp = null
        sslSocket = null
    }

    // ── Protobuf wire encoding ────────────────────────────────────────────
    // Manual encoding for the specific message types needed by the ATV protocol.
    // Field encoding: tag = (field_number << 3) | wire_type
    //   wire_type 0 = varint, wire_type 2 = length-delimited

    /**
     * RemoteMessage { remote_configure { code1: 622, device_info { model: "PrivacyShieldRemote", vendor: "PrivacyShield" } } }
     * Field 1 (remote_configure) = tag 0x0A
     */
    private fun buildConfigureMessage(): ByteArray {
        val deviceInfo = ByteArrayOutputStream().apply {
            // field 1 (model), string
            writeTag(this, 1, WIRE_LEN)
            writeString(this, "PrivacyShieldRemote")
            // field 2 (vendor), string
            writeTag(this, 2, WIRE_LEN)
            writeString(this, "PrivacyShield")
            // field 3 (unknown1), varint = 1
            writeTag(this, 3, WIRE_VARINT)
            writeVarint(this, 1)
            // field 4 (unknown2), string = "1"
            writeTag(this, 4, WIRE_LEN)
            writeString(this, "1")
        }.toByteArray()

        val configure = ByteArrayOutputStream().apply {
            // field 1 (code1 = 622), varint
            writeTag(this, 1, WIRE_VARINT)
            writeVarint(this, 622)
            // field 6 (device_info), length-delimited
            writeTag(this, 6, WIRE_LEN)
            writeVarint(this, deviceInfo.size)
            write(deviceInfo)
        }.toByteArray()

        return ByteArrayOutputStream().apply {
            // RemoteMessage field 1 (remote_configure)
            writeTag(this, 1, WIRE_LEN)
            writeVarint(this, configure.size)
            write(configure)
        }.toByteArray()
    }

    /**
     * RemoteMessage { remote_set_active { active: 1 } }
     * Field 6 (remote_set_active) = tag 0x32
     */
    private fun buildSetActiveMessage(): ByteArray {
        val setActive = ByteArrayOutputStream().apply {
            writeTag(this, 1, WIRE_VARINT)
            writeVarint(this, 1) // active = 1
        }.toByteArray()

        return ByteArrayOutputStream().apply {
            writeTag(this, 6, WIRE_LEN)
            writeVarint(this, setActive.size)
            write(setActive)
        }.toByteArray()
    }

    /**
     * RemoteMessage { remote_key_inject { key_code: keyCode, direction: direction } }
     * Field 8 (remote_key_inject) = tag 0x42
     */
    private fun buildKeyInjectMessage(keyCode: Int, direction: Int): ByteArray {
        val inject = ByteArrayOutputStream().apply {
            writeTag(this, 1, WIRE_VARINT)
            writeVarint(this, keyCode)
            writeTag(this, 2, WIRE_VARINT)
            writeVarint(this, direction)
        }.toByteArray()

        return ByteArrayOutputStream().apply {
            writeTag(this, 8, WIRE_LEN)
            writeVarint(this, inject.size)
            write(inject)
        }.toByteArray()
    }

    /**
     * RemoteMessage { remote_ping_response { val1: val1 } }
     * Field 12 (remote_ping_response) = tag 0x62
     */
    private fun buildPingResponse(val1: Long): ByteArray {
        val pingResp = ByteArrayOutputStream().apply {
            writeTag(this, 1, WIRE_VARINT)
            writeLongVarint(this, val1)
        }.toByteArray()

        return ByteArrayOutputStream().apply {
            writeTag(this, 12, WIRE_LEN)
            writeVarint(this, pingResp.size)
            write(pingResp)
        }.toByteArray()
    }

    // ── Proto primitives ──────────────────────────────────────────────────

    private fun writeTag(out: ByteArrayOutputStream, fieldNumber: Int, wireType: Int) {
        writeVarint(out, (fieldNumber shl 3) or wireType)
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Int) {
        var v = value
        while (v and 0x7F.inv() != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v)
    }

    private fun writeLongVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            out.write(((v and 0x7FL) or 0x80L).toInt())
            v = v ushr 7
        }
        out.write(v.toInt())
    }

    private fun writeString(out: ByteArrayOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        writeVarint(out, bytes.size)
        out.write(bytes)
    }

    private fun parseVarintAt(bytes: ByteArray, startPos: Int): Long {
        var result = 0L
        var shift = 0
        var pos = startPos
        while (pos < bytes.size) {
            val b = bytes[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }

    // ── Message framing ───────────────────────────────────────────────────

    private fun writeMessage(out: DataOutputStream, data: ByteArray) {
        out.writeInt(data.size)
        out.write(data)
    }

    private fun readMessage(inp: DataInputStream): ByteArray {
        val length = inp.readInt()
        if (length <= 0 || length > MAX_MSG_SIZE) return ByteArray(0)
        val data = ByteArray(length)
        inp.readFully(data)
        return data
    }

    // ── TrustManager ─────────────────────────────────────────────────────

    /**
     * Accepts any server certificate during the initial connection.
     * Android TV devices use self-signed certificates; this is the same
     * approach used by the official Android TV Remote app before pairing.
     * After pairing, a persistent certificate can be stored and verified.
     */
    private class TrustAllManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    companion object {
        private const val DIR_SHORT = 0   // key down + key up in one event
        private const val TAG_PING_REQUEST = 0x5A  // field 11, wire type 2
        private const val WIRE_VARINT = 0
        private const val WIRE_LEN = 2
        private const val MAX_MSG_SIZE = 1024 * 64 // 64 KB guard
    }
}
