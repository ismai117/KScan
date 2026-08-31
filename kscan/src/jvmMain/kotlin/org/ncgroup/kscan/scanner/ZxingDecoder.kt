package org.ncgroup.kscan.scanner

import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.toKScanFormat
import org.ncgroup.kscan.format.toZxingFormat
import java.util.EnumMap

// luminances is exposed so a caller decoding a stream of frames can refill it
// rather than allocate one per frame.
internal class GrayLuminanceSource(
    val luminances: ByteArray,
    width: Int,
    height: Int,
) : LuminanceSource(width, height) {

    constructor(width: Int, height: Int) : this(ByteArray(width * height), width, height)

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val res = if (row == null || row.size < width) ByteArray(width) else row
        System.arraycopy(luminances, y * width, res, 0, width)
        return res
    }

    override fun getMatrix(): ByteArray = luminances
}

internal fun zxingReader(codeTypes: List<BarcodeFormat>): MultiFormatReader {
    val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)

    val hasAllFormats =
        codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)

    if (!hasAllFormats) {
        codeTypes.mapNotNull { it.toZxingFormat() }
            .takeIf { it.isNotEmpty() }
            ?.let { hints[DecodeHintType.POSSIBLE_FORMATS] = it }
    }

    hints[DecodeHintType.CHARACTER_SET] = "ISO-8859-1"
    hints[DecodeHintType.TRY_HARDER] = true

    return MultiFormatReader().apply { setHints(hints) }
}

internal fun writeLuminances(pixels: IntArray, luminances: ByteArray) {
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = (pixel shr 16) and 0xff
        val g = (pixel shr 8) and 0xff
        val b = pixel and 0xff

        luminances[i] = ((r + (g shl 1) + b) shr 2).toByte()
    }
}

// Byte segments are preferred over the text, since a barcode's payload is not
// always representable as a string.
internal fun Result.toBarcode(): Barcode {
    @Suppress("UNCHECKED_CAST")
    val segments = resultMetadata
        ?.get(ResultMetadataType.BYTE_SEGMENTS) as? List<ByteArray?>

    return Barcode(
        data = text,
        format = barcodeFormat.toKScanFormat(),
        rawBytes = segments?.firstOrNull() ?: text.toByteArray(Charsets.ISO_8859_1),
    )
}
