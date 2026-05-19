package com.privacyshield.remote.transport

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import com.privacyshield.remote.model.RemoteCommand
import com.privacyshield.remote.model.RemoteDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * IR transport using Android's ConsumerIrManager.
 *
 * Only usable when the device hardware exposes FEATURE_CONSUMER_IR.
 * This transport uses NEC protocol encoding.
 *
 * IMPORTANT: IR codes are manufacturer-specific. The defaults here target
 * Samsung TVs (address 0x07). For other brands, the user must configure
 * the correct address/command codes. A full universal code set is not
 * included since those are proprietary; NEC protocol structure is public.
 */
class IrTransport(private val context: Context) : RemoteTransport {

    private val irManager: ConsumerIrManager? by lazy {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    val isAvailable: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
                && irManager?.hasIrEmitter() == true

    override val isConnected: Boolean
        get() = isAvailable // IR is "connected" when hardware is present

    override suspend fun connect(device: RemoteDevice): ConnectionResult {
        return if (isAvailable) ConnectionResult.Connected
        else ConnectionResult.Error("No IR emitter detected on this device.")
    }

    override suspend fun sendCommand(command: RemoteCommand): CommandResult {
        if (!isAvailable) return CommandResult.Error("IR not available on this device.")
        return withContext(Dispatchers.Default) {
            val code = IR_CODES[command]
                ?: return@withContext CommandResult.Error("No IR code mapped for ${command.label}")
            try {
                val pattern = encodeNEC(code.address, code.command)
                irManager!!.transmit(NEC_CARRIER_FREQUENCY, pattern)
                CommandResult.Sent
            } catch (e: Exception) {
                CommandResult.Error(e.message ?: "IR transmit failed")
            }
        }
    }

    override suspend fun disconnect() {
        // IR is stateless — nothing to close
    }

    // ── NEC Protocol Encoder ──────────────────────────────────────────────

    /**
     * Encodes a command using the NEC IR protocol.
     * Pattern: alternating ON (pulse) / OFF (space) durations in microseconds.
     * Samsung TVs use NEC with 8-bit address and 8-bit command.
     */
    private fun encodeNEC(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()

        // AGC start burst
        pattern += 9000  // 9ms on
        pattern += 4500  // 4.5ms off

        // Address byte (LSB first)
        repeat(8) { bit -> pattern.addNecBit(address, bit) }

        // Inverted address byte (~address)
        repeat(8) { bit -> pattern.addNecBit(address.inv() and 0xFF, bit) }

        // Command byte (LSB first)
        repeat(8) { bit -> pattern.addNecBit(command, bit) }

        // Inverted command byte (~command)
        repeat(8) { bit -> pattern.addNecBit(command.inv() and 0xFF, bit) }

        // Stop bit
        pattern += 562

        return pattern.toIntArray()
    }

    private fun MutableList<Int>.addNecBit(byte: Int, bitIndex: Int) {
        add(562)  // on pulse always 562µs
        add(if (byte and (1 shl bitIndex) != 0) 1688 else 562)
    }

    // ── IR Code Table (Samsung NEC codes as default) ──────────────────────

    data class IrCode(val address: Int, val command: Int)

    companion object {
        private const val NEC_CARRIER_FREQUENCY = 38000  // Hz

        // Samsung TV NEC codes (address = 0x07)
        // For other manufacturers: Sony uses SIRC protocol, LG uses NEC with different address.
        // Users with non-Samsung TVs should note that these codes may not work.
        private val IR_CODES: Map<RemoteCommand, IrCode> = mapOf(
            RemoteCommand.POWER to IrCode(0x07, 0x02),
            RemoteCommand.VOLUME_UP to IrCode(0x07, 0x07),
            RemoteCommand.VOLUME_DOWN to IrCode(0x07, 0x0B),
            RemoteCommand.VOLUME_MUTE to IrCode(0x07, 0x0F),
            RemoteCommand.DPAD_UP to IrCode(0x07, 0x60),
            RemoteCommand.DPAD_DOWN to IrCode(0x07, 0x61),
            RemoteCommand.DPAD_LEFT to IrCode(0x07, 0x65),
            RemoteCommand.DPAD_RIGHT to IrCode(0x07, 0x62),
            RemoteCommand.DPAD_CENTER to IrCode(0x07, 0x68),
            RemoteCommand.BACK to IrCode(0x07, 0x58),
            RemoteCommand.HOME to IrCode(0x07, 0x79),
            RemoteCommand.MENU to IrCode(0x07, 0x1A),
            RemoteCommand.MEDIA_PLAY_PAUSE to IrCode(0x07, 0x47),
            RemoteCommand.MEDIA_PLAY to IrCode(0x07, 0x47),
            RemoteCommand.MEDIA_PAUSE to IrCode(0x07, 0x47),
            RemoteCommand.MEDIA_REWIND to IrCode(0x07, 0x45),
            RemoteCommand.MEDIA_FAST_FORWARD to IrCode(0x07, 0x52),
        )
    }
}
