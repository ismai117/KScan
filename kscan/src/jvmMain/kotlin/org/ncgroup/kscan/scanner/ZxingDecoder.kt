package org.ncgroup.kscan.scanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.BarcodeFormatMapper
import org.ncgroup.kscan.format.wantsEveryFormat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.util.EnumMap
import kotlin.math.floor

internal class GrayLuminanceSource private constructor(
    width: Int,
    height: Int,
    private val luminances: ByteArray,
) : LuminanceSource(width, height) {

    constructor(width: Int, height: Int) : this(width, height, ByteArray(width * height))

    private var widened: FloatArray? = null

    fun writeLuminances(pixels: IntArray) {
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            luminances[i] = ((r + (g shl 1) + b) shr 2).toByte()
        }
    }

    // Reads the image's bytes directly; getRGB would convert every pixel through
    // the colour model first.
    fun writeLuminances(bgr: ByteArray) {
        for (i in luminances.indices) {
            val b = bgr[i * 3].toInt() and 0xff
            val g = bgr[i * 3 + 1].toInt() and 0xff
            val r = bgr[i * 3 + 2].toInt() and 0xff

            luminances[i] = ((r + (g shl 1) + b) shr 2).toByte()
        }
    }

    /** Writes the middle [fraction] of this frame into [target], enlarged to fill it. */
    fun writeMagnifiedCentre(target: GrayLuminanceSource, fraction: Double) {
        val cropWidth = (width * fraction).toInt()
        val cropHeight = (height * fraction).toInt()
        val left = (width - cropWidth) / 2
        val top = (height - cropHeight) / 2

        val widened = widened(target.width * cropHeight)
        val scaleX = cropWidth.toDouble() / target.width

        for (y in 0 until cropHeight) {
            val row = (top + y) * width + left
            val out = y * target.width

            for (x in 0 until target.width) {
                val position = (x + 0.5) * scaleX - 0.5
                val nearest = floor(position).toInt()

                widened[out + x] = cubic(
                    row(row, nearest - 1, cropWidth),
                    row(row, nearest, cropWidth),
                    row(row, nearest + 1, cropWidth),
                    row(row, nearest + 2, cropWidth),
                    (position - nearest).toFloat(),
                )
            }
        }

        val scaleY = cropHeight.toDouble() / target.height

        for (y in 0 until target.height) {
            val position = (y + 0.5) * scaleY - 0.5
            val nearest = floor(position).toInt()
            val weight = (position - nearest).toFloat()
            val out = y * target.width

            for (x in 0 until target.width) {
                val value = cubic(
                    column(widened, target.width, nearest - 1, cropHeight, x),
                    column(widened, target.width, nearest, cropHeight, x),
                    column(widened, target.width, nearest + 1, cropHeight, x),
                    column(widened, target.width, nearest + 2, cropHeight, x),
                    weight,
                )

                target.luminances[out + x] = value.toInt().coerceIn(0, 255).toByte()
            }
        }
    }

    private fun row(start: Int, offset: Int, cropWidth: Int): Float = (luminances[start + offset.coerceIn(0, cropWidth - 1)].toInt() and 0xff).toFloat()

    private fun column(
        widened: FloatArray,
        stride: Int,
        offset: Int,
        cropHeight: Int,
        x: Int,
    ): Float = widened[offset.coerceIn(0, cropHeight - 1) * stride + x]

    private fun widened(size: Int): FloatArray = widened
        ?.takeIf { it.size == size }
        ?: FloatArray(size).also { widened = it }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val res = if (row == null || row.size < width) ByteArray(width) else row
        System.arraycopy(luminances, y * width, res, 0, width)
        return res
    }

    override fun getMatrix(): ByteArray = luminances

    // Lets ZXing retry a frame on its side, which is how a row-scanning 1D reader
    // finds an upright barcode.
    override fun isRotateSupported(): Boolean = true

    override fun rotateCounterClockwise(): LuminanceSource {
        val rotated = ByteArray(luminances.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                rotated[(width - 1 - x) * height + y] = luminances[y * width + x]
            }
        }

        return GrayLuminanceSource(height, width, rotated)
    }
}

internal fun zxingReader(codeTypes: List<BarcodeFormat>): MultiFormatReader {
    val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)

    // Left unset for every format, so ZXing also tries the ones KScan has no name
    // for rather than being narrowed to the table.
    if (!wantsEveryFormat(codeTypes)) {
        BarcodeFormatMapper.toZxingFormats(codeTypes)
            .takeIf { it.isNotEmpty() }
            ?.let { hints[DecodeHintType.POSSIBLE_FORMATS] = it }
    }

    hints[DecodeHintType.CHARACTER_SET] = "ISO-8859-1"
    hints[DecodeHintType.TRY_HARDER] = true

    return MultiFormatReader().apply { setHints(hints) }
}

internal fun MultiFormatReader.decodeOrNull(bitmap: BinaryBitmap): Result? = try {
    decodeWithState(bitmap)
} catch (_: NotFoundException) {
    null
}

// Byte segments are preferred over the text, since a barcode's payload is not
// always representable as a string.
internal fun Result.toBarcode(): Barcode {
    @Suppress("UNCHECKED_CAST")
    val segments = resultMetadata
        ?.get(ResultMetadataType.BYTE_SEGMENTS) as? List<ByteArray?>

    return Barcode(
        data = text,
        format = BarcodeFormatMapper.toAppFormat(barcodeFormat),
        rawBytes = segments?.firstOrNull() ?: text.toByteArray(Charsets.ISO_8859_1),
    )
}

// Half of each side, enlarged back to the frame's own size, so the second pass
// costs the same to decode as the first.
private const val CENTRE_FRACTION = 0.5

internal class FrameDecoder(private val reader: MultiFormatReader) {
    private var frame: GrayLuminanceSource? = null
    private var magnified: GrayLuminanceSource? = null
    private var packedPixels = IntArray(0)

    /**
     * A 1D barcode needs about two pixels per narrow bar, which a small label held
     * up to a webcam does not get, so a frame that reads as empty at its own scale
     * is read again from its enlarged middle.
     */
    fun decode(image: BufferedImage): Result? {
        val frame = frameSource(image.width, image.height)
        frame.write(image)

        reader.decodeOrNull(BinaryBitmap(HybridBinarizer(frame)))?.let { return it }

        val magnified = magnifiedSource(frame.width, frame.height)
        frame.writeMagnifiedCentre(magnified, CENTRE_FRACTION)

        return reader.decodeOrNull(BinaryBitmap(HybridBinarizer(magnified)))
    }

    private fun GrayLuminanceSource.write(image: BufferedImage) {
        val bgr = image.bgrBytes()

        if (bgr != null) {
            writeLuminances(bgr)
            return
        }

        if (packedPixels.size != image.width * image.height) {
            packedPixels = IntArray(image.width * image.height)
        }

        image.getRGB(0, 0, image.width, image.height, packedPixels, 0, image.width)
        writeLuminances(packedPixels)
    }

    private fun frameSource(width: Int, height: Int): GrayLuminanceSource = frame?.takeIf { it.width == width && it.height == height }
        ?: GrayLuminanceSource(width, height).also { frame = it }

    private fun magnifiedSource(width: Int, height: Int): GrayLuminanceSource = magnified?.takeIf { it.width == width && it.height == height }
        ?: GrayLuminanceSource(width, height).also { magnified = it }
}

private fun BufferedImage.bgrBytes(): ByteArray? {
    if (type != BufferedImage.TYPE_3BYTE_BGR) return null

    val buffer = raster.dataBuffer as? DataBufferByte ?: return null

    return buffer.data.takeIf { buffer.numBanks == 1 && it.size == width * height * 3 }
}

// Catmull-Rom interpolation.
private fun cubic(
    before: Float,
    start: Float,
    end: Float,
    after: Float,
    weight: Float,
): Float {
    val squared = weight * weight
    val cubed = squared * weight

    return 0.5f * (
        2 * start +
            (end - before) * weight +
            (2 * before - 5 * start + 4 * end - after) * squared +
            (3 * start - before - 3 * end + after) * cubed
        )
}
