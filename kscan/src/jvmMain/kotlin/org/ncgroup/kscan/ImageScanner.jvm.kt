package org.ncgroup.kscan

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayInputStream
import java.util.EnumMap
import javax.imageio.ImageIO

actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    try {
        val inputStream = ByteArrayInputStream(imageBytes)
        val bufferedImage = ImageIO.read(inputStream)

        if (bufferedImage == null) {
            result(BarcodeResult.OnFailed(Exception("Failed to decode image bytes")))
            return
        }

        val width = bufferedImage.width
        val height = bufferedImage.height
        val pixels = IntArray(width * height)
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width)

        val luminances = ByteArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            luminances[i] = ((r + (g shl 1) + b) shr 2).toByte()
        }

        val source = BufferLuminanceSource(luminances, width, height)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val reader = MultiFormatReader()
        val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)

        val hasAllFormats = codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)
        if (!hasAllFormats) {
            val formats = codeTypes.mapNotNull { it.toZxingFormat() }
            if (formats.isNotEmpty()) {
                hints[DecodeHintType.POSSIBLE_FORMATS] = formats
            }
        }
        hints[DecodeHintType.CHARACTER_SET] = "ISO-8859-1"
        hints[DecodeHintType.TRY_HARDER] = true

        reader.setHints(hints)

        val zxingResult = try {
            reader.decode(binaryBitmap)
        } catch (e: NotFoundException) {
            result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
            return
        }

        val rawBytes = if (zxingResult.resultMetadata?.containsKey(ResultMetadataType.BYTE_SEGMENTS) == true) {
            @Suppress("UNCHECKED_CAST")
            val byteSegments = zxingResult.resultMetadata[ResultMetadataType.BYTE_SEGMENTS] as? List<ByteArray?>
            byteSegments?.firstOrNull() ?: zxingResult.text.toByteArray(Charsets.ISO_8859_1)
        } else {
            zxingResult.text.toByteArray(Charsets.ISO_8859_1)
        }

        val barcode = Barcode(
            data = zxingResult.text,
            format = zxingResult.barcodeFormat.toKScanFormat().toString(),
            rawBytes = rawBytes,
        )

        if (filter(barcode)) {
            result(BarcodeResult.OnSuccess(barcode))
        } else {
            result(BarcodeResult.OnFailed(Exception("Barcode filtered out")))
        }
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(e))
    }
}

private class BufferLuminanceSource(
    private val luminances: ByteArray,
    width: Int,
    height: Int,
) : com.google.zxing.LuminanceSource(width, height) {

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val width = width
        val res = if (row == null || row.size < width) ByteArray(width) else row
        System.arraycopy(luminances, y * width, res, 0, width)
        return res
    }

    override fun getMatrix(): ByteArray = luminances
}
