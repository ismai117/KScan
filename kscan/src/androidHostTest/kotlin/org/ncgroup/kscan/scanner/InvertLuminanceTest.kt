package org.ncgroup.kscan.scanner

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvertLuminanceTest {

    @Test
    fun `GIVEN unpadded rows WHEN inverted THEN every luminance is complemented`() {
        val source = ByteBuffer.wrap(byteArrayOf(0, 1, 2, 3))
        val destination = ByteArray(4 * 3 / 2)

        invertLuminance(source, destination, width = 2, height = 2, rowStride = 2)

        assertContentEquals(
            byteArrayOf(-1, -2, -3, -4),
            destination.copyOfRange(0, 4),
        )
    }

    @Test
    fun `GIVEN padded rows WHEN inverted THEN the padding is skipped`() {
        // Two 2px rows in a buffer whose rows are 4 bytes wide; 9 and 9 are padding.
        val source = ByteBuffer.wrap(byteArrayOf(0, 1, 9, 9, 2, 3, 9, 9))
        val destination = ByteArray(4 * 3 / 2)

        invertLuminance(source, destination, width = 2, height = 2, rowStride = 4)

        assertContentEquals(
            byteArrayOf(-1, -2, -3, -4),
            destination.copyOfRange(0, 4),
        )
    }

    @Test
    fun `GIVEN the extremes WHEN inverted THEN black and white swap`() {
        val source = ByteBuffer.wrap(byteArrayOf(0x00, 0xFF.toByte()))
        val destination = ByteArray(2 * 3 / 2)

        invertLuminance(source, destination, width = 2, height = 1, rowStride = 2)

        assertEquals(0xFF.toByte(), destination[0])
        assertEquals(0x00, destination[1])
    }

    @Test
    fun `GIVEN any frame WHEN inverted THEN the chroma half is neutral`() {
        val source = ByteBuffer.wrap(ByteArray(4))
        val destination = ByteArray(4 * 3 / 2)

        invertLuminance(source, destination, width = 2, height = 2, rowStride = 2)

        assertContentEquals(
            ByteArray(2) { 128.toByte() },
            destination.copyOfRange(4, destination.size),
        )
    }

    @Test
    fun `GIVEN a stride narrower than the frame WHEN inverted THEN it fails rather than reading past a row`() {
        val source = ByteBuffer.wrap(ByteArray(4))

        assertFailsWith<IllegalArgumentException> {
            invertLuminance(source, ByteArray(6), width = 4, height = 1, rowStride = 2)
        }
    }
}
