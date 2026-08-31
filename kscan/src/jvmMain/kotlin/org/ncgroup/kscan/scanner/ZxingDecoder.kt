package org.ncgroup.kscan.scanner

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.BarcodeFormatMapper
import org.ncgroup.kscan.format.wantsEveryFormat
import java.awt.image.BufferedImage
import java.util.EnumMap

internal class GrayLuminanceSource(width: Int, height: Int) : LuminanceSource(width, height) {

    private val luminances = ByteArray(width * height)

    fun writeLuminances(pixels: IntArray) {
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            luminances[i] = ((r + (g shl 1) + b) shr 2).toByte()
        }
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val res = if (row == null || row.size < width) ByteArray(width) else row
        System.arraycopy(luminances, y * width, res, 0, width)
        return res
    }

    override fun getMatrix(): ByteArray = luminances
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

// The source is passed in rather than allocated, so a caller decoding a stream of
// frames can refill one rather than allocate per frame.
internal fun BufferedImage.toBinaryBitmap(
    pixels: IntArray,
    source: GrayLuminanceSource,
): BinaryBitmap {
    getRGB(0, 0, width, height, pixels, 0, width)
    source.writeLuminances(pixels)

    return BinaryBitmap(HybridBinarizer(source))
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
