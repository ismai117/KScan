package org.ncgroup.kscan.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

// ML Kit will not try a light-on-dark barcode itself, so it is handed the negative.
internal class FrameInverter {
    // Safe to reuse: ImageAnalysis withholds the next frame until the current
    // proxy is closed, which is after the inverted scan completes.
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

internal fun invertLuminance(
    source: ByteBuffer,
    destination: ByteArray,
    width: Int,
    height: Int,
    rowStride: Int,
) {
    require(rowStride >= width) { "Invalid Y rowStride: $rowStride, width: $width" }

    val rowBytes = ByteArray(width)

    // The camera pads each row out to rowStride, so rows are read one at a time
    // from their own offset rather than the plane being read as a single run.
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

private const val NEUTRAL_CHROMA = 128.toByte()
