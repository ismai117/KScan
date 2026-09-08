package org.ncgroup.kscan.benchmark.corpus

/** An ARGB_8888 image laid out row by row, so it can be handed straight to `Bitmap.createBitmap`. */
class Frame(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(pixels.size == width * height) {
            "Expected ${width * height} pixels for ${width}x$height, got ${pixels.size}"
        }
    }

    operator fun get(x: Int, y: Int): Int = pixels[y * width + x]

    companion object {
        fun filled(
            width: Int,
            height: Int,
            color: Int,
        ): Frame = Frame(width, height, IntArray(width * height) { color })
    }
}

/**
 * What a warp should show where it has no scene to draw.
 *
 * The symbol is centred with a wide quiet zone, so the corner is the background
 * it was rendered on. Taking it from the frame keeps an inverted or coloured
 * scene from gaining a white wedge when the camera moves.
 */
fun Frame.backgroundColour(): Int = this[0, 0]

const val BLACK: Int = 0xFF000000.toInt()
const val WHITE: Int = 0xFFFFFFFF.toInt()

fun rgb(
    r: Int,
    g: Int,
    b: Int,
): Int = 0xFF shl 24 or (r shl 16) or (g shl 8) or b

internal fun Int.red(): Int = (this shr 16) and 0xFF

internal fun Int.green(): Int = (this shr 8) and 0xFF

internal fun Int.blue(): Int = this and 0xFF

/** Rec. 601 luma, the same weighting a camera's Y plane carries. */
internal fun Int.luma(): Int = (red() * 299 + green() * 587 + blue() * 114) / 1000
