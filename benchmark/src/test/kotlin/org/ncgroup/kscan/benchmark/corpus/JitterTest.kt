package org.ncgroup.kscan.benchmark.corpus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class JitterTest {
    @Test
    fun `GIVEN no jitter THEN the frame is the canonical one`() {
        val sample = BarcodeCorpus.SAMPLES.first { it.condition.name == "baseline" }

        assertEquals(sample.render().pixels.toList(), sample.render(Jitter.NONE).pixels.toList())
        assertTrue(Jitter.NONE.isIdentity)
    }

    @Test
    fun `GIVEN the same frame number THEN the jitter is reproducible`() {
        assertEquals(Jitter.handHeld(seed = 4, frame = 7), Jitter.handHeld(seed = 4, frame = 7))
    }

    @Test
    fun `GIVEN consecutive frames THEN they differ`() {
        val sample = BarcodeCorpus.SAMPLES.first { it.condition.name == "baseline" }

        val first = sample.render(Jitter.handHeld(seed = 1, frame = 0))
        val second = sample.render(Jitter.handHeld(seed = 1, frame = 1))

        assertNotEquals(first.pixels.toList(), second.pixels.toList())
    }

    @Test
    fun `GIVEN no jitter THEN the light sits exactly where the single frame corpus put it`() {
        // The single-frame results were measured before the light was allowed to
        // move, so they only stay comparable while an unjittered frame is the
        // same frame it always was.
        listOf("light-ramp", "light-vignette", "light-glare", "glare-rotated-blurred").forEach { name ->
            val sample = BarcodeCorpus.SAMPLES.first { it.condition.name == name }

            assertEquals(name, sample.render().pixels.toList(), sample.render(Jitter.NONE).pixels.toList())
        }

        assertEquals(0.0, Jitter.NONE.lightShiftX, 0.0)
        assertEquals(0.0, Jitter.NONE.lightShiftY, 0.0)
    }

    @Test
    fun `GIVEN a moving camera THEN a highlight moves further than the scene does`() {
        val jitter = Jitter.handHeld(seed = 5, frame = 2)

        assertTrue(abs(jitter.lightShiftX) > abs(jitter.dx))
        assertEquals(Jitter.SPECULAR_GAIN, jitter.lightShiftX / jitter.dx, 1e-9)
    }

    @Test
    fun `GIVEN a moving camera THEN glare lands somewhere else in the frame`() {
        val sample = BarcodeCorpus.SAMPLES.first { it.condition.name == "light-glare" }

        val first = sample.render(Jitter.handHeld(seed = 2, frame = 0))
        val second = sample.render(Jitter.handHeld(seed = 2, frame = 1))

        assertNotEquals(first.pixels.toList(), second.pixels.toList())
    }

    @Test
    fun `GIVEN one scene THEN its roll is tremor about zero rather than a fixed offset`() {
        // java.util.Random only XORs its seed, so consecutive frame numbers used
        // as seeds hand back nearly the same first value. Drawn that way a scene
        // sat at a constant roll of about a degree, which is a staged angle and
        // not the tremor this claims to model.
        val scenes = BarcodeCorpus.SAMPLES.take(120).map { it.id.hashCode().toLong() }

        val allOneSign = scenes.count { seed ->
            val rolls = (0 until 30).map { Jitter.handHeld(seed, it).rotationDegrees }
            rolls.all { it > 0.0 } || rolls.all { it < 0.0 }
        }

        assertEquals("Scenes whose every frame rolled the same way", 0, allOneSign)
    }

    @Test
    fun `GIVEN many frames THEN the roll matches the sigma it documents`() {
        val rolls = (0 until 4000).map { Jitter.handHeld(seed = 12, frame = it).rotationDegrees }
        val mean = rolls.average()
        val sigma = kotlin.math.sqrt(rolls.sumOf { (it - mean) * (it - mean) } / rolls.size)

        assertEquals(0.0, mean, 0.12)
        assertEquals(Jitter.ROTATION_SIGMA, sigma, 0.12)
    }

    @Test
    fun `GIVEN hand tremor THEN it stays small enough not to re-aim the camera`() {
        // The result would be the model's rather than the decoder's if a frame
        // could drift far enough to turn one rotation condition into another.
        val rotations = (0 until 400).map { abs(Jitter.handHeld(seed = 9, frame = it).rotationDegrees) }

        assertTrue("Tremor reached ${rotations.max()} degrees", rotations.max() < 8.0)
        assertTrue(rotations.average() < 2.0)
    }

    @Test
    fun `GIVEN hand tremor THEN the frame stays within a few pixels`() {
        val shifts = (0 until 400).flatMap {
            val jitter = Jitter.handHeld(seed = 3, frame = it)
            listOf(abs(jitter.dx), abs(jitter.dy))
        }

        // Half a degree of pointing wobble at roughly 21 pixels per degree.
        assertTrue("Shift reached ${shifts.max()} pixels", shifts.max() < 50.0)
        assertTrue(shifts.average() < 12.0)
    }
}
