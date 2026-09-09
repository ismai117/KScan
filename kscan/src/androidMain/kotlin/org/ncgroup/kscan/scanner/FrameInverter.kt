package org.ncgroup.kscan.scanner

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Hands the decoder the negative of a frame.
 *
 * zxing-cpp's own `tryInvert` is offered only to the readers that declare
 * support for reversed reflectance, which is QR, Data Matrix and Aztec; a linear
 * symbology printed light on dark is never tried. Inverting the luminance here
 * is what keeps those readable.
 *
 * The result is an eight bit alpha bitmap, which zxing-cpp reads as a plain
 * luminance plane, so this costs one byte per pixel rather than four.
 */
internal class FrameInverter {
    // Safe to reuse: ImageAnalysis withholds the next frame until the current
    // proxy is closed, which is after the inverted scan completes.
    private var bitmap: Bitmap? = null
    private var buffer: ByteArray? = null

    @OptIn(ExperimentalGetImage::class)
    fun invert(imageProxy: ImageProxy): Bitmap {
        val mediaImage = imageProxy.image ?: throw IllegalArgumentException("Image is null")
        require(mediaImage.planes.isNotEmpty()) { "Image has no planes" }

        val width = mediaImage.width
        val height = mediaImage.height

        val target = bitmap?.takeIf { it.width == width && it.height == height }
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8).also { bitmap = it }

        val destinationStride = target.rowBytes
        val bytes = buffer?.takeIf { it.size == destinationStride * height }
            ?: ByteArray(destinationStride * height).also { buffer = it }

        val yPlane = mediaImage.planes[0]

        invertLuminance(
            source = yPlane.buffer.duplicate(),
            destination = bytes,
            width = width,
            height = height,
            sourceStride = yPlane.rowStride,
            destinationStride = destinationStride,
        )

        target.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))

        return target
    }
}

internal fun invertLuminance(
    source: ByteBuffer,
    destination: ByteArray,
    width: Int,
    height: Int,
    sourceStride: Int,
    destinationStride: Int = width,
) {
    require(sourceStride >= width) { "Invalid Y rowStride: $sourceStride, width: $width" }
    require(destinationStride >= width) { "Invalid destination stride: $destinationStride, width: $width" }
    require(destination.size >= destinationStride * height) {
        "Destination holds ${destination.size} bytes, needs ${destinationStride * height}"
    }

    val rowBytes = ByteArray(width)

    // Both sides pad their rows out to a stride of their own, so rows are copied
    // one at a time from and to their own offsets rather than as a single run.
    for (row in 0 until height) {
        source.position(row * sourceStride)
        source.get(rowBytes, 0, width)

        val outBase = row * destinationStride
        for (col in 0 until width) {
            destination[outBase + col] = (rowBytes[col].toInt() xor 0xFF).toByte()
        }
    }
}
