package org.ncgroup.kscan.benchmark.corpus

import com.google.zxing.BarcodeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The corpus is the measuring instrument, so it is checked before it is used: a
 * degradation that quietly destroyed every symbol would read as a decoder
 * regression rather than as a broken generator.
 */
class CorpusTest {
    @Test
    fun `GIVEN the corpus THEN every format KScan exposes is covered exactly once`() {
        val formats = BarcodeCorpus.SPECIMENS.map { it.format }

        assertEquals(13, formats.size)
        assertEquals(formats.size, formats.distinct().size)
    }

    @Test
    fun `GIVEN the corpus THEN every specimen is rendered under every condition`() {
        assertEquals(
            BarcodeCorpus.SPECIMENS.size * Conditions.ALL.size,
            BarcodeCorpus.SAMPLES.size,
        )
    }

    @Test
    fun `GIVEN the conditions THEN each is named once`() {
        val names = Conditions.ALL.map { it.name }

        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun `GIVEN a payload THEN it encodes in its own symbology`() {
        BarcodeCorpus.SPECIMENS.forEach { specimen ->
            val matrix = BarcodeCorpus.encode(specimen)

            assertTrue("${specimen.format} produced an empty matrix", matrix.width > 0 && matrix.height > 0)
        }
    }

    @Test
    fun `GIVEN every sample THEN it renders to a full frame`() {
        BarcodeCorpus.SAMPLES.forEach { sample ->
            val frame = sample.render()

            assertEquals(sample.id, BarcodeCorpus.FRAME_WIDTH, frame.width)
            assertEquals(sample.id, BarcodeCorpus.FRAME_HEIGHT, frame.height)
        }
    }

    @Test
    fun `GIVEN a degrading condition THEN it actually changes the frame`() {
        val specimen = BarcodeCorpus.SPECIMENS.first { it.format == BarcodeFormat.QR_CODE }
        val baseline = Sample(specimen, Conditions.ALL.first { it.name == "baseline" }).render()

        Conditions.ALL
            .filter { it.name != "baseline" }
            .forEach { condition ->
                val degraded = Sample(specimen, condition).render()

                assertNotEquals(
                    "${condition.name} left the frame untouched",
                    baseline.pixels.toList(),
                    degraded.pixels.toList(),
                )
            }
    }

    @Test
    fun `GIVEN a distant condition THEN it renders fewer pixels per module than a near one`() {
        val specimen = BarcodeCorpus.SPECIMENS.first { it.format == BarcodeFormat.QR_CODE }

        fun scaleOf(name: String) = Sample(specimen, Conditions.ALL.first { it.name == name }).pixelsPerModule

        assertTrue(scaleOf("distance-near") > scaleOf("baseline"))
        assertTrue(scaleOf("baseline") > scaleOf("distance-far"))
        assertTrue(scaleOf("distance-far") >= scaleOf("distance-very-far"))
    }
}
