package org.ncgroup.kscan.benchmark.corpus

import java.util.Random
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A 3x3 homogeneous transform, row major.
 *
 * Rotation, shear and perspective are all expressed as one of these so that a
 * single warp covers every geometric condition in the corpus.
 */
class Matrix3(private val m: DoubleArray) {
    init {
        require(m.size == 9) { "A 3x3 matrix needs 9 values, got ${m.size}" }
    }

    operator fun times(other: Matrix3): Matrix3 {
        val out = DoubleArray(9)
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                var sum = 0.0
                for (k in 0 until 3) {
                    sum += m[row * 3 + k] * other.m[k * 3 + col]
                }
                out[row * 3 + col] = sum
            }
        }
        return Matrix3(out)
    }

    fun map(
        x: Double,
        y: Double,
    ): DoubleArray {
        val w = m[6] * x + m[7] * y + m[8]
        // A point on the horizon has no finite image; it is pushed far off frame
        // so the sampler treats it as background rather than dividing by zero.
        if (w == 0.0) return doubleArrayOf(Double.MAX_VALUE, Double.MAX_VALUE)

        return doubleArrayOf(
            (m[0] * x + m[1] * y + m[2]) / w,
            (m[3] * x + m[4] * y + m[5]) / w,
        )
    }

    fun invert(): Matrix3 {
        val a = m
        val c0 = a[4] * a[8] - a[5] * a[7]
        val c1 = a[5] * a[6] - a[3] * a[8]
        val c2 = a[3] * a[7] - a[4] * a[6]
        val determinant = a[0] * c0 + a[1] * c1 + a[2] * c2

        require(determinant != 0.0) { "Matrix is not invertible" }

        return Matrix3(
            doubleArrayOf(
                c0 / determinant,
                (a[2] * a[7] - a[1] * a[8]) / determinant,
                (a[1] * a[5] - a[2] * a[4]) / determinant,
                c1 / determinant,
                (a[0] * a[8] - a[2] * a[6]) / determinant,
                (a[2] * a[3] - a[0] * a[5]) / determinant,
                c2 / determinant,
                (a[1] * a[6] - a[0] * a[7]) / determinant,
                (a[0] * a[4] - a[1] * a[3]) / determinant,
            ),
        )
    }

    companion object {
        val IDENTITY: Matrix3 = Matrix3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0))

        fun of(vararg values: Double): Matrix3 = Matrix3(values.copyOf())
    }
}

object ImageOps {
    /**
     * Resamples [source] through the inverse of [forward], which maps source
     * pixels to their place in the result. Anything the result draws from outside
     * the source becomes [background].
     */
    fun warp(
        source: Frame,
        forward: Matrix3,
        background: Int,
    ): Frame {
        val inverse = forward.invert()
        val out = IntArray(source.width * source.height)
        val maxX = (source.width - 1).toDouble()
        val maxY = (source.height - 1).toDouble()

        var index = 0
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val point = inverse.map(x.toDouble(), y.toDouble())
                // Snapped before the bounds check: a quarter turn lands on whole
                // pixels in exact arithmetic but a hair either side of them in
                // floating point, and a hair below zero would read as off frame.
                val sx = snap(point[0])
                val sy = snap(point[1])

                out[index++] =
                    if (sx < 0.0 || sy < 0.0 || sx > maxX || sy > maxY) {
                        background
                    } else {
                        sampleBilinear(source, sx, sy)
                    }
            }
        }

        return Frame(source.width, source.height, out)
    }

    /**
     * Adds light at that position, then clamps.
     *
     * Glare and over-exposure add photons rather than scaling them, so on a
     * printed black and white symbol only this lifts the dark modules and takes
     * the contrast away; a multiplier would leave pure black and pure white
     * exactly where they were.
     */
    fun addLight(
        source: Frame,
        amount: (x: Int, y: Int) -> Double,
    ): Frame {
        val out = IntArray(source.pixels.size)

        var index = 0
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val pixel = source.pixels[index]
                val added = amount(x, y)
                out[index++] =
                    rgb(
                        clampChannel(pixel.red() + added),
                        clampChannel(pixel.green() + added),
                        clampChannel(pixel.blue() + added),
                    )
            }
        }

        return Frame(source.width, source.height, out)
    }

    /** Multiplies every channel by the gain at that position, then clamps. */
    fun illuminate(
        source: Frame,
        gain: (x: Int, y: Int) -> Double,
    ): Frame {
        val out = IntArray(source.pixels.size)

        var index = 0
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val pixel = source.pixels[index]
                val factor = gain(x, y)
                out[index++] =
                    rgb(
                        clampChannel(pixel.red() * factor),
                        clampChannel(pixel.green() * factor),
                        clampChannel(pixel.blue() * factor),
                    )
            }
        }

        return Frame(source.width, source.height, out)
    }

    /**
     * Three box passes, which is the usual cheap stand-in for a Gaussian of the
     * same radius and is what a defocused lens does to a barcode's edges.
     */
    fun blur(
        source: Frame,
        radius: Int,
        passes: Int = 3,
    ): Frame {
        if (radius <= 0) return source

        var frame = source
        repeat(passes) {
            frame = boxBlurPass(boxBlurPass(frame, radius, horizontal = true), radius, horizontal = false)
        }
        return frame
    }

    /**
     * The photographic negative.
     *
     * Neither decoder reads a light-on-dark linear symbol on its own: ML Kit has
     * no inversion pass at all, and zxing-cpp's `tryInvert` is offered only to
     * the readers that declare support for reversed reflectance, which is QR,
     * Data Matrix and Aztec but never a linear one. Both therefore need to be
     * handed the negative, which is what KScan's analyzer already does.
     */
    fun invert(source: Frame): Frame {
        val out = IntArray(source.pixels.size)

        for (index in source.pixels.indices) {
            val pixel = source.pixels[index]
            out[index] = rgb(255 - pixel.red(), 255 - pixel.green(), 255 - pixel.blue())
        }

        return Frame(source.width, source.height, out)
    }

    /** Additive Gaussian noise, the dominant artefact of a short exposure in low light. */
    fun noise(
        source: Frame,
        sigma: Double,
        seed: Long,
    ): Frame {
        if (sigma <= 0.0) return source

        val random = Random(seed)
        val out = IntArray(source.pixels.size)

        for (index in source.pixels.indices) {
            val pixel = source.pixels[index]
            val offset = random.nextGaussian() * sigma
            out[index] =
                rgb(
                    clampChannel(pixel.red() + offset),
                    clampChannel(pixel.green() + offset),
                    clampChannel(pixel.blue() + offset),
                )
        }

        return Frame(source.width, source.height, out)
    }

    /** Rotation in the image plane, about the frame's centre. */
    fun rotation(
        width: Int,
        height: Int,
        degrees: Double,
    ): Matrix3 {
        val radians = Math.toRadians(degrees)
        val cos = cos(radians)
        val sin = sin(radians)
        val cx = (width - 1) / 2.0
        val cy = (height - 1) / 2.0

        return Matrix3.of(
            cos, -sin, cx - cos * cx + sin * cy,
            sin, cos, cy - sin * cx - cos * cy,
            0.0, 0.0, 1.0,
        )
    }

    /**
     * The frame as it would be photographed with the paper turned away from the
     * camera: rotated about the vertical axis by [yawDegrees] and the horizontal
     * axis by [pitchDegrees], then projected through a pinhole.
     */
    fun tilt(
        width: Int,
        height: Int,
        yawDegrees: Double,
        pitchDegrees: Double,
    ): Matrix3 {
        val cx = (width - 1) / 2.0
        val cy = (height - 1) / 2.0
        val distance = maxOf(width, height) * 1.6
        val yaw = Math.toRadians(yawDegrees)
        val pitch = Math.toRadians(pitchDegrees)

        val corners =
            listOf(
                0.0 to 0.0,
                (width - 1).toDouble() to 0.0,
                (width - 1).toDouble() to (height - 1).toDouble(),
                0.0 to (height - 1).toDouble(),
            ).map { (x, y) ->
                val px = x - cx
                val py = y - cy

                // Yaw about Y, then pitch about X.
                val x1 = px * cos(yaw)
                val z1 = -px * sin(yaw)
                val y2 = py * cos(pitch) - z1 * sin(pitch)
                val z2 = py * sin(pitch) + z1 * cos(pitch)

                val scale = distance / (distance + z2)
                doubleArrayOf(cx + x1 * scale, cy + y2 * scale)
            }

        return squareToQuad(corners) * rectToSquare(width, height)
    }

    /** Maps the unit square's corners, in the order top left, top right, bottom right, bottom left. */
    internal fun squareToQuad(corners: List<DoubleArray>): Matrix3 {
        require(corners.size == 4) { "A quad needs 4 corners, got ${corners.size}" }

        val (x0, y0) = corners[0].let { it[0] to it[1] }
        val (x1, y1) = corners[1].let { it[0] to it[1] }
        val (x2, y2) = corners[2].let { it[0] to it[1] }
        val (x3, y3) = corners[3].let { it[0] to it[1] }

        val dx1 = x1 - x2
        val dx2 = x3 - x2
        val dx3 = x0 - x1 + x2 - x3
        val dy1 = y1 - y2
        val dy2 = y3 - y2
        val dy3 = y0 - y1 + y2 - y3

        if (dx3 == 0.0 && dy3 == 0.0) {
            return Matrix3.of(
                x1 - x0, x3 - x0, x0,
                y1 - y0, y3 - y0, y0,
                0.0, 0.0, 1.0,
            )
        }

        val denominator = dx1 * dy2 - dy1 * dx2
        val g = (dx3 * dy2 - dy3 * dx2) / denominator
        val h = (dx1 * dy3 - dy1 * dx3) / denominator

        return Matrix3.of(
            x1 - x0 + g * x1, x3 - x0 + h * x3, x0,
            y1 - y0 + g * y1, y3 - y0 + h * y3, y0,
            g, h, 1.0,
        )
    }

    internal fun rectToSquare(
        width: Int,
        height: Int,
    ): Matrix3 = Matrix3.of(
        1.0 / (width - 1), 0.0, 0.0,
        0.0, 1.0 / (height - 1), 0.0,
        0.0, 0.0, 1.0,
    )

    private fun sampleBilinear(
        source: Frame,
        sx: Double,
        sy: Double,
    ): Int {
        val x0 = floor(sx).toInt()
        val y0 = floor(sy).toInt()
        val x1 = minOf(x0 + 1, source.width - 1)
        val y1 = minOf(y0 + 1, source.height - 1)
        val fx = sx - x0
        val fy = sy - y0

        val topLeft = source[x0, y0]
        val topRight = source[x1, y0]
        val bottomLeft = source[x0, y1]
        val bottomRight = source[x1, y1]

        fun channel(extract: Int.() -> Int): Int {
            val top = topLeft.extract() * (1 - fx) + topRight.extract() * fx
            val bottom = bottomLeft.extract() * (1 - fx) + bottomRight.extract() * fx
            return clampChannel(top * (1 - fy) + bottom * fy)
        }

        return rgb(channel { red() }, channel { green() }, channel { blue() })
    }

    private fun boxBlurPass(
        source: Frame,
        radius: Int,
        horizontal: Boolean,
    ): Frame {
        val out = IntArray(source.pixels.size)
        val outer = if (horizontal) source.height else source.width
        val inner = if (horizontal) source.width else source.height
        val window = radius * 2 + 1

        for (o in 0 until outer) {
            for (i in 0 until inner) {
                var r = 0
                var g = 0
                var b = 0
                for (offset in -radius..radius) {
                    val j = (i + offset).coerceIn(0, inner - 1)
                    val pixel = if (horizontal) source[j, o] else source[o, j]
                    r += pixel.red()
                    g += pixel.green()
                    b += pixel.blue()
                }
                val index = if (horizontal) o * source.width + i else i * source.width + o
                out[index] = rgb(r / window, g / window, b / window)
            }
        }

        return Frame(source.width, source.height, out)
    }

    private fun snap(value: Double): Double {
        val nearest = Math.round(value).toDouble()
        return if (Math.abs(value - nearest) < SNAP_EPSILON) nearest else value
    }

    private fun clampChannel(value: Double): Int = value.toInt().coerceIn(0, 255)

    private const val SNAP_EPSILON = 1e-6
}

/** Falls off from 1.0 at the centre to [edge] at the corners. */
fun vignette(
    width: Int,
    height: Int,
    edge: Double,
): (Int, Int) -> Double {
    val cx = width / 2.0
    val cy = height / 2.0
    val maxDistance = hypot(cx, cy)

    return { x, y ->
        val distance = hypot(x - cx, y - cy) / maxDistance
        1.0 - (1.0 - edge) * distance * distance
    }
}

/** A linear ramp across the frame, from [start] on the left to [end] on the right. */
fun ramp(
    width: Int,
    start: Double,
    end: Double,
): (Int, Int) -> Double = { x, _ -> start + (end - start) * x / (width - 1).toDouble() }

/**
 * A blown-out highlight, as a reflection off a glossy label, peaking at [peak]
 * levels of added brightness at its centre and fading to nothing at [radius].
 */
fun glare(
    centerX: Int,
    centerY: Int,
    radius: Double,
    peak: Double,
): (Int, Int) -> Double = { x, y ->
    val distance = hypot((x - centerX).toDouble(), (y - centerY).toDouble()) / radius
    if (distance >= 1.0) 0.0 else peak * (1.0 - distance * distance)
}

infix fun ((Int, Int) -> Double).and(other: (Int, Int) -> Double): (Int, Int) -> Double = { x, y -> this(x, y) * other(x, y) }
