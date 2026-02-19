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

    // region Helpers

    private fun buildByteModePayload(data: ByteArray): ByteArray {
        val bits = mutableListOf<Int>()
        bits.addAll(listOf(0, 1, 0, 0)) // Mode: 0100 (byte)
        addBits(bits, data.size, 8)
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
