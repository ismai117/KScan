package org.ncgroup.kscan.scanner

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RepeatedDetectionTest {
    @Test
    fun `GIVEN one sighting THEN it is not accepted`() {
        assertFalse(RepeatedDetection().accept("A"))
    }

    @Test
    fun `GIVEN the same value twice THEN it is accepted`() {
        val repeated = RepeatedDetection()

        assertFalse(repeated.accept("A"))
        assertTrue(repeated.accept("A"))
    }

    @Test
    fun `GIVEN different values THEN neither is accepted`() {
        val repeated = RepeatedDetection()

        assertFalse(repeated.accept("A"))
        assertFalse(repeated.accept("B"))
    }

    @Test
    fun `GIVEN a reset THEN counting starts again`() {
        val repeated = RepeatedDetection()

        repeated.accept("A")
        repeated.reset()

        assertFalse(repeated.accept("A"))
    }
}
