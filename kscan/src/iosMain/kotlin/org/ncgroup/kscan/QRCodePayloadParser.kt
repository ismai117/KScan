package org.ncgroup.kscan

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreImage.CIQRCodeDescriptor
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * Parses raw bytes from CIQRCodeDescriptor.errorCorrectedPayload.
 *
 * Only handles byte mode segments (which can contain null bytes).
 * Returns null for other modes, falling back to stringValue which works fine for them.
 */
internal object QRCodePayloadParser {

    private const val MODE_BYTE = 4

    @OptIn(ExperimentalForeignApi::class)
    fun extractRawBytes(descriptor: CIQRCodeDescriptor): ByteArray? {
        val payload = descriptor.errorCorrectedPayload
        val codewords = payload.toByteArray()
        if (codewords.isEmpty()) return null
        return decodeDataStream(codewords)
    }

    internal fun decodeDataStream(codewords: ByteArray): ByteArray? {
        // Try both possible byte-mode length sizes:
        // - QR versions 1–9  => 8 bits
        // - QR versions 10–40 => 16 bits
        for (countBits in listOf(8, 16)) {
            val reader = BitReader(codewords)

            if (!reader.hasAvailable(4)) continue
            val mode = reader.readBits(4)

            // Only handle byte mode - other modes can't have null bytes
            if (mode != MODE_BYTE) return null

            if (!reader.hasAvailable(countBits)) continue
            val count = reader.readBits(countBits)

            if (count <= 0) continue

            // Optional sanity guard (remove if you prefer)
            if (count > 4096) continue
            if (!reader.hasAvailable(count * 8)) continue

            val result = ByteArray(count)
            repeat(count) { i ->
                result[i] = reader.readBits(8).toByte()
            }
            return result
        }

        return null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        if (length == 0) return byteArrayOf()

        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return bytes
    }

    private class BitReader(private val data: ByteArray) {
        private var bitPosition = 0

        fun hasAvailable(bits: Int): Boolean = (data.size * 8 - bitPosition) >= bits

        fun readBits(count: Int): Int {
            var result = 0
            repeat(count) {
                val byteIndex = bitPosition / 8
                val bitIndex = 7 - (bitPosition % 8)
                if (byteIndex < data.size) {
                    result = (result shl 1) or ((data[byteIndex].toInt() shr bitIndex) and 1)
                }
                bitPosition++
            }
            return result
        }
    }
}
