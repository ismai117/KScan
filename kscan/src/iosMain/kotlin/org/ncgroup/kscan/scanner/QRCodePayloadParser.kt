package org.ncgroup.kscan.scanner

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreImage.CIQRCodeDescriptor
import platform.Foundation.NSData
import platform.posix.memcpy

/**
 * Parses raw bytes from CIQRCodeDescriptor.errorCorrectedPayload.
 *
 * Iterates over all QR data segments (byte, alphanumeric, numeric) and concatenates
 * their decoded content. Byte segments are appended as raw bytes (preserving null
 * bytes); alphanumeric and numeric segments are decoded per the QR spec and appended
 * as their ISO-8859-1 bytes.
 *
 * Returns a non-null result only when at least one byte segment was present — the
 * sole reason this parser exists is to preserve null bytes that
 * AVMetadataMachineReadableCodeObject.stringValue would silently truncate. For
 * purely alphanumeric/numeric QRs (or Kanji/unknown modes), returns null so the
 * caller falls back to stringValue.
 */
internal object QRCodePayloadParser {

    private const val MODE_TERMINATOR = 0
    private const val MODE_NUMERIC = 1
    private const val MODE_ALPHANUMERIC = 2
    private const val MODE_BYTE = 4

    private const val ALPHANUMERIC_TABLE =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ \$%*+-./:"

    @OptIn(ExperimentalForeignApi::class)
    fun extractRawBytes(descriptor: CIQRCodeDescriptor): ByteArray? {
        val payload = descriptor.errorCorrectedPayload
        val codewords = payload.toByteArray()
        if (codewords.isEmpty()) return null
        return decodeDataStream(codewords)
    }

    /**
     * Character-count indicator widths per QR version group (ISO/IEC 18004 Table 3).
     *
     * Within a single QR symbol all segments share the same version, so all three
     * widths must come from the same row. We try each row in turn; the first one
     * that decodes cleanly and contains at least one byte segment wins.
     */
    private data class VersionWidths(
        val byteCount: Int,
        val alphaCount: Int,
        val numericCount: Int,
    )

    private val VERSION_GROUPS = listOf(
        VersionWidths(byteCount = 8, alphaCount = 9, numericCount = 10), // v1–9
        VersionWidths(byteCount = 16, alphaCount = 11, numericCount = 12), // v10–26
        VersionWidths(byteCount = 16, alphaCount = 13, numericCount = 14), // v27–40
    )

    internal fun decodeDataStream(codewords: ByteArray): ByteArray? {
        for (widths in VERSION_GROUPS) {
            val result = tryDecodeWithWidths(codewords, widths)
            if (result != null) return result
        }
        return null
    }

    private fun tryDecodeWithWidths(
        codewords: ByteArray,
        widths: VersionWidths,
    ): ByteArray? {
        val reader = BitReader(codewords)
        val result = mutableListOf<Byte>()
        var hadByteSegment = false
        var aborted = false

        while (reader.hasAvailable(4)) {
            val mode = reader.readBits(4)
            if (mode == MODE_TERMINATOR) break

            when (mode) {
                MODE_BYTE -> {
                    if (!reader.hasAvailable(widths.byteCount)) {
                        aborted = true
                        break
                    }
                    val count = reader.readBits(widths.byteCount)
                    if (count !in 1..4096 || !reader.hasAvailable(count * 8)) {
                        aborted = true
                        break
                    }
                    hadByteSegment = true
                    repeat(count) { result.add(reader.readBits(8).toByte()) }
                }

                MODE_ALPHANUMERIC -> {
                    if (!reader.hasAvailable(widths.alphaCount)) {
                        aborted = true
                        break
                    }
                    val count = reader.readBits(widths.alphaCount)
                    val decoded = decodeAlphanumeric(reader, count)
                    if (decoded == null) {
                        aborted = true
                        break
                    }
                    appendIsoLatin1(result, decoded)
                }

                MODE_NUMERIC -> {
                    if (!reader.hasAvailable(widths.numericCount)) {
                        aborted = true
                        break
                    }
                    val count = reader.readBits(widths.numericCount)
                    val decoded = decodeNumeric(reader, count)
                    if (decoded == null) {
                        aborted = true
                        break
                    }
                    appendIsoLatin1(result, decoded)
                }

                else -> {
                    // Kanji, ECI, structured-append, FNC1, or unknown — can't
                    // safely decode here. Abort this attempt; the next version
                    // group may still succeed. If all attempts fail we return
                    // null and the caller falls back to stringValue.
                    aborted = true
                    break
                }
            }
        }

        return if (!aborted && hadByteSegment && result.isNotEmpty()) {
            result.toByteArray()
        } else {
            null
        }
    }

    private fun decodeAlphanumeric(reader: BitReader, count: Int): String? {
        if (count < 0) return null
        val sb = StringBuilder(count)
        val pairs = count / 2
        val remainder = count % 2
        repeat(pairs) {
            if (!reader.hasAvailable(11)) return null
            val v = reader.readBits(11)
            val hi = v / 45
            val lo = v % 45
            if (hi !in ALPHANUMERIC_TABLE.indices ||
                lo !in ALPHANUMERIC_TABLE.indices
            ) {
                return null
            }
            sb.append(ALPHANUMERIC_TABLE[hi])
            sb.append(ALPHANUMERIC_TABLE[lo])
        }
        if (remainder == 1) {
            if (!reader.hasAvailable(6)) return null
            val v = reader.readBits(6)
            if (v !in ALPHANUMERIC_TABLE.indices) return null
            sb.append(ALPHANUMERIC_TABLE[v])
        }
        return sb.toString()
    }

    private fun decodeNumeric(reader: BitReader, count: Int): String? {
        if (count < 0) return null
        val sb = StringBuilder(count)
        val triples = count / 3
        val rem = count % 3
        repeat(triples) {
            if (!reader.hasAvailable(10)) return null
            val value = reader.readBits(10)
            // QR spec: each 10-bit group encodes exactly 0..999.
            if (value > 999) return null
            sb.append(value.toString().padStart(3, '0'))
        }
        when (rem) {
            2 -> {
                if (!reader.hasAvailable(7)) return null
                val value = reader.readBits(7)
                // QR spec: 7-bit tail encodes exactly 0..99.
                if (value > 99) return null
                sb.append(value.toString().padStart(2, '0'))
            }

            1 -> {
                if (!reader.hasAvailable(4)) return null
                val value = reader.readBits(4)
                // QR spec: 4-bit tail encodes exactly 0..9.
                if (value > 9) return null
                sb.append(value.toString())
            }
        }
        return sb.toString()
    }

    private fun appendIsoLatin1(dest: MutableList<Byte>, s: String) {
        // ISO-8859-1: each code point 0..0xFF maps to a single byte of the same value.
        // Alphanumeric/numeric segments only produce ASCII, which is a subset.
        for (ch in s) {
            dest.add((ch.code and 0xFF).toByte())
        }
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
