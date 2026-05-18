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

    internal fun decodeDataStream(codewords: ByteArray): ByteArray? {
        // The byte-mode length field width depends on QR version:
        // - QR versions 1–9  => 8 bits
        // - QR versions 10–40 => 16 bits
        // Alphanumeric/numeric have their own fixed widths per the spec (9/10 bits
        // for v1-9, which is all we attempt to support here).
        for (byteCountBits in listOf(8, 16)) {
            val reader = BitReader(codewords)
            val result = mutableListOf<Byte>()
            var hadByteSegment = false
            var aborted = false

            while (reader.hasAvailable(4)) {
                val mode = reader.readBits(4)
                if (mode == MODE_TERMINATOR) break

                when (mode) {
                    MODE_BYTE -> {
                        if (!reader.hasAvailable(byteCountBits)) {
                            aborted = true
                            break
                        }
                        val count = reader.readBits(byteCountBits)
                        if (count !in 1..4096 || !reader.hasAvailable(count * 8)) {
                            aborted = true
                            break
                        }
                        hadByteSegment = true
                        repeat(count) { result.add(reader.readBits(8).toByte()) }
                    }

                    MODE_ALPHANUMERIC -> {
                        if (!reader.hasAvailable(9)) {
                            aborted = true
                            break
                        }
                        val count = reader.readBits(9)
                        val sb = StringBuilder()
                        val pairs = count / 2
                        val remainder = count % 2
                        var ok = true
                        repeat(pairs) {
                            if (!ok) return@repeat
                            if (!reader.hasAvailable(11)) {
                                ok = false
                                return@repeat
                            }
                            val v = reader.readBits(11)
                            val hi = v / 45
                            val lo = v % 45
                            if (hi !in ALPHANUMERIC_TABLE.indices ||
                                lo !in ALPHANUMERIC_TABLE.indices
                            ) {
                                ok = false
                                return@repeat
                            }
                            sb.append(ALPHANUMERIC_TABLE[hi])
                            sb.append(ALPHANUMERIC_TABLE[lo])
                        }
                        if (ok && remainder == 1) {
                            if (!reader.hasAvailable(6)) {
                                ok = false
                            } else {
                                val v = reader.readBits(6)
                                if (v !in ALPHANUMERIC_TABLE.indices) {
                                    ok = false
                                } else {
                                    sb.append(ALPHANUMERIC_TABLE[v])
                                }
                            }
                        }
                        if (!ok) {
                            aborted = true
                            break
                        }
                        appendIsoLatin1(result, sb.toString())
                    }

                    MODE_NUMERIC -> {
                        if (!reader.hasAvailable(10)) {
                            aborted = true
                            break
                        }
                        val count = reader.readBits(10)
                        val sb = StringBuilder()
                        val triples = count / 3
                        val rem = count % 3
                        var ok = true
                        repeat(triples) {
                            if (!ok) return@repeat
                            if (!reader.hasAvailable(10)) {
                                ok = false
                                return@repeat
                            }
                            sb.append(reader.readBits(10).toString().padStart(3, '0'))
                        }
                        if (ok) {
                            when (rem) {
                                2 -> if (reader.hasAvailable(7)) {
                                    sb.append(reader.readBits(7).toString().padStart(2, '0'))
                                } else {
                                    ok = false
                                }
                                1 -> if (reader.hasAvailable(4)) {
                                    sb.append(reader.readBits(4).toString())
                                } else {
                                    ok = false
                                }
                            }
                        }
                        if (!ok) {
                            aborted = true
                            break
                        }
                        appendIsoLatin1(result, sb.toString())
                    }

                    else -> {
                        // Kanji, ECI, structured-append, FNC1, or unknown — can't
                        // safely decode here. Abort this attempt; if the mode was
                        // caused by trying the wrong byte-count width, the next
                        // attempt can still succeed. Otherwise we'll fall back to
                        // stringValue after all attempts fail.
                        aborted = true
                        break
                    }
                }
            }

            if (!aborted && hadByteSegment && result.isNotEmpty()) {
                return result.toByteArray()
            }
        }

        return null
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
