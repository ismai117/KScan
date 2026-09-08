package org.ncgroup.kscan.benchmark.corpus

/**
 * One way a barcode can present itself to a camera.
 *
 * [sizeFraction] is the share of the frame the symbol spans, which stands in for
 * distance; [degrade] is everything the optics and the light do to it.
 */
class Condition(
    val name: String,
    val group: String,
    val sizeFraction: Double = DEFAULT_SIZE_FRACTION,
    val foreground: Int = BLACK,
    val background: Int = WHITE,
    val degrade: (Frame) -> Frame = { it },
) {
    override fun toString(): String = name

    companion object {
        const val DEFAULT_SIZE_FRACTION: Double = 0.55
    }
}

private fun rotated(degrees: Double): (Frame) -> Frame = { frame ->
    ImageOps.warp(frame, ImageOps.rotation(frame.width, frame.height, degrees), WHITE)
}

private fun tilted(
    yaw: Double,
    pitch: Double,
): (Frame) -> Frame = { frame ->
    ImageOps.warp(frame, ImageOps.tilt(frame.width, frame.height, yaw, pitch), WHITE)
}

private fun lit(field: (Frame) -> ((Int, Int) -> Double)): (Frame) -> Frame = { frame ->
    ImageOps.illuminate(frame, field(frame))
}

private fun gain(factor: Double): (Frame) -> Frame = lit { { _, _ -> factor } }

private fun added(field: (Frame) -> ((Int, Int) -> Double)): (Frame) -> Frame = { frame ->
    ImageOps.addLight(frame, field(frame))
}

private fun blurred(radius: Int): (Frame) -> Frame = { frame -> ImageOps.blur(frame, radius) }

private fun noisy(
    sigma: Double,
    seed: Long,
): (Frame) -> Frame = { frame -> ImageOps.noise(frame, sigma, seed) }

private infix fun ((Frame) -> Frame).then(next: (Frame) -> Frame): (Frame) -> Frame = { frame -> next(this(frame)) }

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
                degrade = added { { _, _ -> 110.0 } },
            ),
            Condition(
                name = "light-ramp",
                group = "lighting",
                degrade = lit { frame -> ramp(frame.width, 0.22, 1.35) },
            ),
            Condition(
                name = "light-vignette",
                group = "lighting",
                degrade = lit { frame -> vignette(frame.width, frame.height, 0.25) },
            ),
            Condition(
                name = "light-glare",
                group = "lighting",
                degrade = added { frame -> glare(frame.width / 2, frame.height / 2, frame.height * 0.30, 210.0) },
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
                    added { frame -> glare(frame.width / 2, frame.height / 2, frame.height * 0.35, 170.0) } then
                    blurred(2),
            ),
        )
}
