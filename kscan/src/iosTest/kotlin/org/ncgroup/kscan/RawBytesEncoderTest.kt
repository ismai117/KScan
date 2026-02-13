package org.ncgroup.kscan

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RawBytesEncoderTest {

    @Test
    fun `GIVEN ascii string WHEN stringToRawBytes THEN returns correct bytes`() {
        val result = stringToRawBytes("ABC")

        assertEquals(3, result.size)
        assertContentEquals(byteArrayOf(0x41, 0x42, 0x43), result)
    }

    @Test
    fun `GIVEN high byte value WHEN stringToRawBytes THEN no extra bytes inserted`() {
        val input = "\u0080"  // byte 0x80
        val result = stringToRawBytes(input)

        assertEquals(1, result.size)
        assertEquals(0x80.toByte(), result[0])
    }

    @Test
    fun `GIVEN multiple high bytes WHEN stringToRawBytes THEN preserves all bytes`() {
        val input = "\u0080\u00A5\u00FF"  // bytes 0x80, 0xA5, 0xFF
        val result = stringToRawBytes(input)

        assertEquals(3, result.size)
        assertContentEquals(byteArrayOf(0x80.toByte(), 0xA5.toByte(), 0xFF.toByte()), result)
    }

    @Test
    fun `GIVEN mixed ascii and high bytes WHEN stringToRawBytes THEN preserves all bytes`() {
        val input = "A\u0080B\u00FFC"  // A, 0x80, B, 0xFF, C
        val result = stringToRawBytes(input)

        assertEquals(5, result.size)
        assertContentEquals(
            byteArrayOf(0x41, 0x80.toByte(), 0x42, 0xFF.toByte(), 0x43),
            result
        )
    }

    @Test
    fun `GIVEN empty string WHEN stringToRawBytes THEN returns empty array`() {
        val result = stringToRawBytes("")

        assertEquals(0, result.size)
    }
}
