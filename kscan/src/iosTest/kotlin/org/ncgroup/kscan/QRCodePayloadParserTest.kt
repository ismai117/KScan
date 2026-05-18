package org.ncgroup.kscan

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QRCodePayloadParserTest {

    @Test
    fun `GIVEN byte mode payload WHEN decodeDataStream THEN returns correct bytes`() {
        val payload = buildByteModePayload(byteArrayOf(0x41, 0x42, 0x43))

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertEquals(3, result.size)
        assertContentEquals(byteArrayOf(0x41, 0x42, 0x43), result)
    }

    @Test
    fun `GIVEN byte mode with null byte WHEN decodeDataStream THEN preserves null byte`() {
        val payload = buildByteModePayload(
            byteArrayOf('H'.code.toByte(), 'i'.code.toByte(), 0x00, '!'.code.toByte())
        )

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertEquals(4, result.size)
        assertEquals('H'.code.toByte(), result[0])
        assertEquals('i'.code.toByte(), result[1])
        assertEquals(0x00.toByte(), result[2])
        assertEquals('!'.code.toByte(), result[3])
    }

    @Test
    fun `GIVEN numeric mode WHEN decodeDataStream THEN returns null`() {
        val payload = buildNumericModePayload("123")

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNull(result) // Numeric can't have null bytes, use stringValue fallback
    }

    @Test
    fun `GIVEN alphanumeric mode WHEN decodeDataStream THEN returns null`() {
        val payload = buildAlphanumericModePayload("AB")

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNull(result) // Alphanumeric can't have null bytes, use stringValue fallback
    }

    @Test
    fun `GIVEN empty payload WHEN decodeDataStream THEN returns null`() {
        assertNull(QRCodePayloadParser.decodeDataStream(byteArrayOf()))
    }

    @Test
    fun `GIVEN hello world with null byte WHEN decodeDataStream THEN full data preserved`() {
        val payload = buildByteModePayload(
            byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x00, 0x57, 0x6F, 0x72, 0x6C, 0x64)
        )

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertEquals(11, result.size)
        assertEquals('H'.code.toByte(), result[0])
        assertEquals(0x00.toByte(), result[5]) // null byte preserved
        assertEquals('W'.code.toByte(), result[6])
        assertEquals('d'.code.toByte(), result[10])
    }

    @Test
    fun `GIVEN byte then alphanumeric segments WHEN decodeDataStream THEN concatenated`() {
        // Real-world pattern: byte segment with a URL prefix (lowercase requires byte
        // mode) + alphanumeric segment with an uppercase serial (cheaper in
        // alphanumeric mode).
        val url = "https://example.com/item?sn="
        val serial = "ABC123-240101-001"
        val payload = buildMultiSegmentPayload(
            listOf(
                Segment.Byte(url.encodeToByteArray()),
                Segment.Alphanumeric(serial),
            ),
        )

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertEquals(url + serial, result.decodeToString())
    }

    @Test
    fun `GIVEN byte mode payload with 16-bit count WHEN decodeDataStream THEN retries and decodes`() {
        val data = "QR v10+ byte segment".encodeToByteArray()
        val payload = buildByteModePayload(data, countBits = 16)

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertContentEquals(data, result)
    }

    @Test
    fun `GIVEN byte then numeric segments WHEN decodeDataStream THEN concatenated`() {
        val prefix = "ID:"
        val digits = "1234567" // exercises triples + remainder=1 (4-bit tail)
        val payload = buildMultiSegmentPayload(
            listOf(
                Segment.Byte(prefix.encodeToByteArray()),
                Segment.Numeric(digits),
            ),
        )

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertEquals(prefix + digits, result.decodeToString())
    }

    @Test
    fun `GIVEN byte numeric remainder 2 WHEN decodeDataStream THEN concatenated`() {
        // remainder=2 exercises the 7-bit numeric tail.
        val payload = buildMultiSegmentPayload(
            listOf(
                Segment.Byte("X".encodeToByteArray()),
                Segment.Numeric("12345"),
            ),
        )

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertEquals("X12345", result.decodeToString())
    }

    @Test
    fun `GIVEN alphanumeric with odd length WHEN decodeDataStream alongside byte THEN concatenated`() {
        // "ABC" => one 11-bit pair + one 6-bit trailing char.
        val payload = buildMultiSegmentPayload(
            listOf(
                Segment.Byte("u=".encodeToByteArray()),
                Segment.Alphanumeric("ABC"),
            ),
        )

        val result = QRCodePayloadParser.decodeDataStream(payload)

        assertNotNull(result)
        assertEquals("u=ABC", result.decodeToString())
    }

    @Test
    fun `GIVEN kanji mode WHEN decodeDataStream THEN returns null`() {
        // Mode 1000 (8) = Kanji. The branch should bail out unconditionally.
        val bits = mutableListOf<Int>()
        bits.addAll(listOf(1, 0, 0, 0)) // Kanji mode
        addBits(bits, 1, 8) // a plausible count field; never read
        bits.addAll(listOf(0, 0, 0, 0))
        val payload = packBits(bits)

        assertNull(QRCodePayloadParser.decodeDataStream(payload))
    }

    // region Helpers

    private sealed class Segment {
        class Byte(val data: ByteArray) : Segment()
        class Alphanumeric(val text: String) : Segment()
        class Numeric(val digits: String) : Segment()
    }

    private fun buildMultiSegmentPayload(segments: List<Segment>): ByteArray {
        val bits = mutableListOf<Int>()
        for (seg in segments) {
            when (seg) {
                is Segment.Byte -> {
                    bits.addAll(listOf(0, 1, 0, 0)) // Mode: 0100
                    addBits(bits, seg.data.size, 8)
                    seg.data.forEach { addBits(bits, it.toInt() and 0xFF, 8) }
                }
                is Segment.Alphanumeric -> {
                    val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ \$%*+-./:"
                    bits.addAll(listOf(0, 0, 1, 0)) // Mode: 0010
                    addBits(bits, seg.text.length, 9)
                    var i = 0
                    while (i + 2 <= seg.text.length) {
                        addBits(
                            bits,
                            chars.indexOf(seg.text[i]) * 45 + chars.indexOf(seg.text[i + 1]),
                            11,
                        )
                        i += 2
                    }
                    if (seg.text.length - i == 1) {
                        addBits(bits, chars.indexOf(seg.text[i]), 6)
                    }
                }
                is Segment.Numeric -> {
                    bits.addAll(listOf(0, 0, 0, 1)) // Mode: 0001
                    addBits(bits, seg.digits.length, 10)
                    var i = 0
                    while (i + 3 <= seg.digits.length) {
                        addBits(bits, seg.digits.substring(i, i + 3).toInt(), 10)
                        i += 3
                    }
                    when (seg.digits.length - i) {
                        2 -> addBits(bits, seg.digits.substring(i).toInt(), 7)
                        1 -> addBits(bits, seg.digits.substring(i).toInt(), 4)
                    }
                }
            }
        }
        bits.addAll(listOf(0, 0, 0, 0)) // Terminator
        return packBits(bits)
    }

    private fun buildByteModePayload(data: ByteArray, countBits: Int = 8): ByteArray {
        val bits = mutableListOf<Int>()
        bits.addAll(listOf(0, 1, 0, 0)) // Mode: 0100 (byte)
        addBits(bits, data.size, countBits)
        data.forEach { addBits(bits, it.toInt() and 0xFF, 8) }
        bits.addAll(listOf(0, 0, 0, 0)) // Terminator
        return packBits(bits)
    }

    private fun buildNumericModePayload(digits: String): ByteArray {
        val bits = mutableListOf<Int>()
        bits.addAll(listOf(0, 0, 0, 1)) // Mode: 0001 (numeric)
        addBits(bits, digits.length, 10)
        var i = 0
        while (i + 3 <= digits.length) {
            addBits(bits, digits.substring(i, i + 3).toInt(), 10)
            i += 3
        }
        if (digits.length - i == 2) addBits(bits, digits.substring(i).toInt(), 7)
        else if (digits.length - i == 1) addBits(bits, digits.substring(i).toInt(), 4)
        bits.addAll(listOf(0, 0, 0, 0))
        return packBits(bits)
    }

    private fun buildAlphanumericModePayload(text: String): ByteArray {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ \$%*+-./:"
        val bits = mutableListOf<Int>()
        bits.addAll(listOf(0, 0, 1, 0)) // Mode: 0010 (alphanumeric)
        addBits(bits, text.length, 9)
        var i = 0
        while (i + 2 <= text.length) {
            addBits(bits, chars.indexOf(text[i]) * 45 + chars.indexOf(text[i + 1]), 11)
            i += 2
        }
        if (text.length - i == 1) addBits(bits, chars.indexOf(text[i]), 6)
        bits.addAll(listOf(0, 0, 0, 0))
        return packBits(bits)
    }

    private fun addBits(bits: MutableList<Int>, value: Int, count: Int) {
        for (i in (count - 1) downTo 0) bits.add((value shr i) and 1)
    }

    private fun packBits(bits: List<Int>): ByteArray {
        return bits.chunked(8).map { chunk ->
            chunk.foldIndexed(0) { idx, acc, bit -> acc or (bit shl (7 - idx)) }.toByte()
        }.toByteArray()
    }

    // endregion
}
