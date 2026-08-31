package org.ncgroup.kscan

import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.common.HybridBinarizer
import org.ncgroup.kscan.scanner.GrayLuminanceSource
import org.ncgroup.kscan.scanner.toBarcode
import org.ncgroup.kscan.scanner.writeLuminances
import org.ncgroup.kscan.scanner.zxingReader
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

public actual fun scanImage(
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

        val source = GrayLuminanceSource(width, height)
        writeLuminances(pixels, source.luminances)

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = zxingReader(codeTypes)

        val zxingResult = try {
            reader.decode(binaryBitmap)
        } catch (e: NotFoundException) {
            result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
            return
        }

        val barcode = zxingResult.toBarcode()

        if (filter(barcode)) {
            result(BarcodeResult.OnSuccess(barcode))
        } else {
            result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
        }
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(e))
    }
}
