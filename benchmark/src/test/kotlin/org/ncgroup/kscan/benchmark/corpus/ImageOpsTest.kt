package org.ncgroup.kscan.benchmark.corpus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageOpsTest {
    private fun checkerboard(
        width: Int = 16,
        height: Int = 16,
    ): Frame = Frame(
        width,
        height,
        IntArray(width * height) { if ((it % width + it / width) % 2 == 0) BLACK else WHITE },
    )

    @Test
    fun `GIVEN a quarter turn applied four times THEN the frame comes back`() {
        val source = checkerboard()
        var frame = source

        repeat(4) {
            frame = ImageOps.warp(frame, ImageOps.rotation(frame.width, frame.height, 90.0), WHITE)
        }

        assertEquals(source.pixels.toList(), frame.pixels.toList())
    }

    @Test
    fun `GIVEN no tilt THEN the frame is unchanged`() {
        val source = checkerboard()

        val frame = ImageOps.warp(source, ImageOps.tilt(source.width, source.height, 0.0, 0.0), WHITE)

        assertEquals(source.pixels.toList(), frame.pixels.toList())
    }

    @Test
    fun `GIVEN a yaw THEN the far edge is drawn shorter than the near edge`() {
        val width = 64
        val height = 64
        val matrix = ImageOps.tilt(width, height, 40.0, 0.0)

        val leftEdge = matrix.map(0.0, 0.0)[1] - matrix.map(0.0, (height - 1).toDouble())[1]
        val rightEdge = matrix.map((width - 1).toDouble(), 0.0)[1] - matrix.map((width - 1).toDouble(), (height - 1).toDouble())[1]

        assertNotEquals(Math.abs(leftEdge), Math.abs(rightEdge), 0.5)
    }

    @Test
    fun `GIVEN a gain THEN every channel scales by it`() {
        val source = Frame(1, 1, intArrayOf(rgb(100, 100, 100)))

        val frame = ImageOps.illuminate(source) { _, _ -> 0.5 }

        assertEquals(50, frame.pixels[0].red())
    }

    @Test
    fun `GIVEN a gain that overflows THEN the channel clips at white`() {
        val source = Frame(1, 1, intArrayOf(rgb(200, 200, 200)))

        val frame = ImageOps.illuminate(source) { _, _ -> 4.0 }

        assertEquals(255, frame.pixels[0].red())
    }

    @Test
    fun `GIVEN a blur THEN edges lose contrast`() {
        val source = checkerboard()

        val frame = ImageOps.blur(source, radius = 2)
        val spread = frame.pixels.maxOf { it.luma() } - frame.pixels.minOf { it.luma() }

        assertTrue("Blur left the checkerboard at full contrast", spread < 255)
    }

    @Test
    fun `GIVEN the same seed THEN noise is reproducible`() {
        val source = Frame.filled(8, 8, rgb(128, 128, 128))

        val first = ImageOps.noise(source, sigma = 20.0, seed = 7)
        val second = ImageOps.noise(source, sigma = 20.0, seed = 7)
        val other = ImageOps.noise(source, sigma = 20.0, seed = 8)

        assertEquals(first.pixels.toList(), second.pixels.toList())
        assertNotEquals(first.pixels.toList(), other.pixels.toList())
    }

    @Test
    fun `GIVEN a vignette THEN the corners are darker than the centre`() {
        val field = vignette(100, 100, edge = 0.25)

        assertTrue(field(50, 50) > field(0, 0))
        assertEquals(1.0, field(50, 50), 0.01)
    }

    @Test
    fun `GIVEN a ramp THEN it runs from start to end across the width`() {
        val field = ramp(101, start = 0.2, end = 1.2)

        assertEquals(0.2, field(0, 0), 0.001)
        assertEquals(1.2, field(100, 0), 0.001)
    }

    @Test
    fun `GIVEN glare THEN it adds light only inside its radius`() {
        val field = glare(centerX = 50, centerY = 50, radius = 10.0, peak = 200.0)

        assertEquals(200.0, field(50, 50), 0.001)
        assertEquals(0.0, field(90, 50), 0.001)
    }

    @Test
    fun `GIVEN added light THEN a black module stops being black`() {
        val source = Frame(1, 1, intArrayOf(BLACK))

        val frame = ImageOps.addLight(source) { _, _ -> 110.0 }

        assertEquals(110, frame.pixels[0].red())
    }
}
