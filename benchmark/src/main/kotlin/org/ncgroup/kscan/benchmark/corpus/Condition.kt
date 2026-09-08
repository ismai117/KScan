package org.ncgroup.kscan.benchmark.corpus

/**
 * One way a barcode can present itself to a camera.
 *
 * [sizeFraction] is the share of the frame the symbol spans, which stands in for
 * distance; [degrade] is everything the optics and the light do to it.
 *
 * A degradation is handed the frame's [Jitter] because not everything it models
 * is nailed to the sensor. Vignetting is, being a property of the lens; a shadow
 * travels with the label it falls on, and a specular highlight travels faster
 * than either.
 */
class Condition(
    val name: String,
    val group: String,
    val sizeFraction: Double = DEFAULT_SIZE_FRACTION,
    val foreground: Int = BLACK,
    val background: Int = WHITE,
    val degrade: Degradation = { frame, _ -> frame },
) {
    override fun toString(): String = name

    companion object {
        const val DEFAULT_SIZE_FRACTION: Double = 0.55
    }
}

/** What a condition does to a frame, given how the camera was moving for it. */
typealias Degradation = (Frame, Jitter) -> Frame

private fun rotated(degrees: Double): Degradation = { frame, _ ->
    ImageOps.warp(frame, ImageOps.rotation(frame.width, frame.height, degrees), frame.backgroundColour())
}

private fun tilted(
    yaw: Double,
    pitch: Double,
): Degradation = { frame, _ ->
    ImageOps.warp(frame, ImageOps.tilt(frame.width, frame.height, yaw, pitch), frame.backgroundColour())
}

private fun lit(field: (Frame, Jitter) -> ((Int, Int) -> Double)): Degradation = { frame, jitter ->
    ImageOps.illuminate(frame, field(frame, jitter))
}

private fun gain(factor: Double): Degradation = lit { _, _ -> { _, _ -> factor } }

private fun added(field: (Frame, Jitter) -> ((Int, Int) -> Double)): Degradation = { frame, jitter ->
    ImageOps.addLight(frame, field(frame, jitter))
}

private fun blurred(radius: Int): Degradation = { frame, _ -> ImageOps.blur(frame, radius) }

private fun noisy(
    sigma: Double,
    seed: Long,
): Degradation = { frame, _ -> ImageOps.noise(frame, sigma, seed) }

private infix fun Degradation.then(next: Degradation): Degradation = { frame, jitter -> next(this(frame, jitter), jitter) }

/**
 * The conditions every format is put through.
 *
 * The axes are the ones a hand-held scan actually varies: how far away the code
 * is, how it is turned, how it is lit, how sharp it is and what colours it is
 * printed in. The last group combines them, since a real bad frame is rarely bad
 * in only one way.
 */
object Conditions {
    val ALL: List<Condition> =
        listOf(
            Condition(name = "baseline", group = "baseline"),
            // Distance: the symbol shrinks until there are too few pixels per module.
            Condition(name = "distance-near", group = "distance", sizeFraction = 0.85),
            Condition(name = "distance-mid", group = "distance", sizeFraction = 0.35),
            Condition(name = "distance-far", group = "distance", sizeFraction = 0.20),
            Condition(name = "distance-very-far", group = "distance", sizeFraction = 0.12),
            // Rotation in the image plane.
            Condition(name = "rotation-15", group = "rotation", degrade = rotated(15.0)),
            Condition(name = "rotation-30", group = "rotation", degrade = rotated(30.0)),
            Condition(name = "rotation-45", group = "rotation", degrade = rotated(45.0)),
            Condition(name = "rotation-90", group = "rotation", degrade = rotated(90.0)),
            Condition(name = "rotation-180", group = "rotation", degrade = rotated(180.0)),
            Condition(name = "rotation-270", group = "rotation", degrade = rotated(270.0)),
            // Out of plane: the label turned away from the camera.
            Condition(name = "tilt-yaw-30", group = "tilt", degrade = tilted(30.0, 0.0)),
            Condition(name = "tilt-yaw-50", group = "tilt", degrade = tilted(50.0, 0.0)),
            Condition(name = "tilt-pitch-30", group = "tilt", degrade = tilted(0.0, 30.0)),
            Condition(name = "tilt-pitch-50", group = "tilt", degrade = tilted(0.0, 50.0)),
            Condition(name = "tilt-both-35", group = "tilt", degrade = tilted(35.0, 35.0)),
            // Light level and evenness.
            Condition(name = "light-dim", group = "lighting", degrade = gain(0.35)),
            Condition(
                name = "light-very-dim",
                group = "lighting",
                degrade = gain(0.18) then noisy(6.0, seed = 11),
            ),
            Condition(
                name = "light-over-exposed",
                group = "lighting",
                degrade = added { _, _ -> { _, _ -> 110.0 } },
            ),
            Condition(
                name = "light-ramp",
                group = "lighting",
                degrade = lit { frame, jitter -> ramp(frame.width, 0.22, 1.35, shift = jitter.dx) },
            ),
            Condition(
                name = "light-vignette",
                group = "lighting",
                // Vignetting belongs to the lens, so it alone stays put while the scene moves.
                degrade = lit { frame, _ -> vignette(frame.width, frame.height, 0.25) },
            ),
            Condition(
                name = "light-glare",
                group = "lighting",
                degrade = added { frame, jitter ->
                    glare(
                        centerX = (frame.width / 2 + jitter.lightShiftX).toInt(),
                        centerY = (frame.height / 2 + jitter.lightShiftY).toInt(),
                        radius = frame.height * 0.30,
                        peak = 210.0,
                    )
                },
            ),
            // Ink and substrate.
            Condition(
                name = "contrast-low",
                group = "contrast",
                foreground = rgb(0x70, 0x70, 0x70),
                background = rgb(0xA8, 0xA8, 0xA8),
            ),
            Condition(name = "inverted", group = "contrast", foreground = WHITE, background = BLACK),
            Condition(
                name = "colour-blue-on-yellow",
                group = "contrast",
                foreground = rgb(0x10, 0x2A, 0x8C),
                background = rgb(0xFF, 0xE0, 0x30),
            ),
            Condition(
                name = "colour-red-on-white",
                group = "contrast",
                foreground = rgb(0xC0, 0x20, 0x20),
                background = WHITE,
            ),
            Condition(
                name = "colour-green-on-black",
                group = "contrast",
                foreground = rgb(0x30, 0xC0, 0x50),
                background = rgb(0x10, 0x10, 0x10),
            ),
            // Focus.
            Condition(name = "blur-1", group = "blur", degrade = blurred(1)),
            Condition(name = "blur-3", group = "blur", degrade = blurred(3)),
            Condition(name = "blur-6", group = "blur", degrade = blurred(6)),
            // Sensor noise.
            Condition(name = "noise-10", group = "noise", degrade = noisy(10.0, seed = 21)),
            Condition(name = "noise-25", group = "noise", degrade = noisy(25.0, seed = 22)),
            // What a bad frame actually looks like.
            Condition(
                name = "dim-blurred-noisy",
                group = "combined",
                degrade = gain(0.30) then blurred(2) then noisy(8.0, seed = 31),
            ),
            Condition(
                name = "rotated-dim",
                group = "combined",
                degrade = rotated(20.0) then gain(0.32),
            ),
            Condition(
                name = "tilted-far",
                group = "combined",
                sizeFraction = 0.22,
                degrade = tilted(30.0, 15.0),
            ),
            Condition(
                name = "glare-rotated-blurred",
                group = "combined",
                degrade =
                rotated(35.0) then
                    added { frame, jitter ->
                        glare(
                            centerX = (frame.width / 2 + jitter.lightShiftX).toInt(),
                            centerY = (frame.height / 2 + jitter.lightShiftY).toInt(),
                            radius = frame.height * 0.35,
                            peak = 170.0,
                        )
                    } then
                    blurred(2),
            ),
        )
}
