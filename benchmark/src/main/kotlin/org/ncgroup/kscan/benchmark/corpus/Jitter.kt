package org.ncgroup.kscan.benchmark.corpus

import java.util.Random

/**
 * How much a hand-held camera differs from one frame to the next.
 *
 * A scanner sees about thirty frames a second and needs one of them, so a
 * condition it reads three times in ten is not a condition it fails. What decides
 * that is whether consecutive frames differ enough to matter, and the honest
 * amount is small: this models someone holding the phone still, not someone
 * re-aiming it.
 *
 * That distinction is the whole result. If the jitter were allowed to swing wide
 * enough, a barcode held at 30 degrees would drift through 15 and be read, which
 * would be the model answering the question rather than the decoder.
 */
data class Jitter(
    val rotationDegrees: Double = 0.0,
    val dx: Double = 0.0,
    val dy: Double = 0.0,
    val exposure: Double = 1.0,
    val noiseSigma: Double = 0.0,
    val seed: Long = 0,
) {
    /**
     * How far a specular highlight slides, which is about twice what the scene
     * does: a mirror-like reflection tracks the bisector of the light and the
     * camera, so half a degree of camera movement sweeps it a full degree. It is
     * why tilting a phone slightly is enough to clear glare off a label.
     */
    val lightShiftX: Double get() = dx * SPECULAR_GAIN

    val lightShiftY: Double get() = dy * SPECULAR_GAIN

    val isIdentity: Boolean
        get() = rotationDegrees == 0.0 && dx == 0.0 && dy == 0.0 && exposure == 1.0 && noiseSigma == 0.0

    /** The camera's small movement, applied to the scene before the conditions act on it. */
    fun aim(frame: Frame): Frame {
        if (rotationDegrees == 0.0 && dx == 0.0 && dy == 0.0) return frame

        val rotation = ImageOps.rotation(frame.width, frame.height, rotationDegrees)
        val translation = Matrix3.of(1.0, 0.0, dx, 0.0, 1.0, dy, 0.0, 0.0, 1.0)

        return ImageOps.warp(frame, translation * rotation, frame.backgroundColour())
    }

    /** The sensor's own variation, which lands after whatever the scene did. */
    fun expose(frame: Frame): Frame {
        val exposed = if (exposure == 1.0) frame else ImageOps.illuminate(frame) { _, _ -> exposure }

        return if (noiseSigma <= 0.0) exposed else ImageOps.noise(exposed, noiseSigma, seed)
    }

    companion object {
        val NONE: Jitter = Jitter()

        /**
         * Tremor while deliberately steadying a phone: 1.5 degrees of roll, half
         * a degree of pointing wobble, 3% of exposure, and the read noise that
         * makes two frames of a still scene differ at all, all one sigma.
         */
        fun handHeld(
            seed: Long,
            frame: Int,
        ): Jitter {
            val random = Random(mix(seed * 31L + frame))

            return Jitter(
                rotationDegrees = random.nextGaussian() * ROTATION_SIGMA,
                dx = random.nextGaussian() * TRANSLATION_SIGMA,
                dy = random.nextGaussian() * TRANSLATION_SIGMA,
                exposure = 1.0 + random.nextGaussian() * EXPOSURE_SIGMA,
                noiseSigma = READ_NOISE_SIGMA,
                seed = random.nextLong(),
            )
        }

        const val ROTATION_SIGMA: Double = 1.5

        /**
         * Pointing wobble, in pixels. A phone camera sees about 60 degrees across
         * a 1280 pixel frame, so a degree of yaw shifts the image by roughly 21
         * pixels; this is the half degree a hand holds to.
         */
        const val TRANSLATION_SIGMA: Double = 10.0

        const val EXPOSURE_SIGMA: Double = 0.03
        const val READ_NOISE_SIGMA: Double = 3.0

        /** A highlight moves at twice the rate of the surface it sits on. */
        const val SPECULAR_GAIN: Double = 2.0

        /**
         * Scrambles the seed before the generator sees it.
         *
         * `java.util.Random` only XORs its seed, so seeds one apart, which is what
         * consecutive frames of one scene produce, hand back nearly the same first
         * value. Drawn straight, a scene's roll would come out as a fixed offset
         * of about a degree rather than as tremor about the angle it was staged
         * at, and the replay would not be measuring what it says it measures.
         */
        private fun mix(value: Long): Long {
            var z = value + -0x61c8864680b583ebL
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }
    }
}
