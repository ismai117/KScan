package org.ncgroup.kscan.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

/**
 * Reads a frame as its photographic negative, so that a light-on-dark barcode
 * arrives at the decoder the way round it expects. ML Kit will not make that
 * second pass itself.
 */
internal class FrameInverter {
    /**
     * Reused across frames. Inverting allocates roughly 1.5 bytes per pixel, which
     * at 1080p is about 3 MB, and only one frame is ever inverted at a time:
     * ImageAnalysis does not deliver the next frame until the current proxy is
     * closed, which happens after the inverted scan completes.
     */
    private var buffer: ByteArray? = null

    @OptIn(ExperimentalGetImage::class)
    fun invert(imageProxy: ImageProxy): InputImage {
        val mediaImage = imageProxy.image ?: throw IllegalArgumentException("Image is null")
        require(mediaImage.planes.isNotEmpty()) { "Image has no planes" }

        val width = mediaImage.width
        val height = mediaImage.height
        val nv21Size = width * height * 3 / 2
        val nv21Bytes = buffer?.takeIf { it.size == nv21Size }
            ?: ByteArray(nv21Size).also { buffer = it }

        val yPlane = mediaImage.planes[0]

        invertLuminance(
            source = yPlane.buffer.duplicate(),
            destination = nv21Bytes,
            width = width,
            height = height,
            rowStride = yPlane.rowStride,
        )

        return InputImage.fromByteArray(
            nv21Bytes,
            width,
            height,
            imageProxy.imageInfo.rotationDegrees,
            InputImage.IMAGE_FORMAT_NV21,
        )
    }
}

/**
 * Writes the negative of [source]'s luminance plane into [destination] as NV21.
 *
 * The camera pads each row out to [rowStride] bytes, so a row is read from its own
 * offset rather than the whole plane being read as one run. [destination]'s chroma
 * half is filled with the neutral value, which leaves a grayscale image.
 */
internal fun invertLuminance(
    source: ByteBuffer,
    destination: ByteArray,
    width: Int,
    height: Int,
    rowStride: Int,
) {
    require(rowStride >= width) { "Invalid Y rowStride: $rowStride, width: $width" }

    val rowBytes = ByteArray(width)

    // Bulk-read one row at a time, then invert into output (fewer ByteBuffer.get() calls)
    for (row in 0 until height) {
        source.position(row * rowStride)
        source.get(rowBytes, 0, width)

        val outBase = row * width
        for (col in 0 until width) {
            destination[outBase + col] = (rowBytes[col].toInt() xor 0xFF).toByte()
        }
    }

    // Neutral chroma for grayscale in NV21 (VU interleaved)
    destination.fill(NEUTRAL_CHROMA, width * height, destination.size)
}

/** Half the 0..255 range: no colour either way. */
private const val NEUTRAL_CHROMA = 128.toByte()
