package org.ncgroup.kscan

import com.google.zxing.NotFoundException
import org.ncgroup.kscan.format.isRequestedFormat
import org.ncgroup.kscan.scanner.GrayLuminanceSource
import org.ncgroup.kscan.scanner.toBarcode
import org.ncgroup.kscan.scanner.toBinaryBitmap
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

        val pixels = IntArray(bufferedImage.width * bufferedImage.height)
        val source = GrayLuminanceSource(bufferedImage.width, bufferedImage.height)

        val binaryBitmap = bufferedImage.toBinaryBitmap(pixels, source)
        val reader = zxingReader(codeTypes)

        val zxingResult = try {
            reader.decode(binaryBitmap)
        } catch (e: NotFoundException) {
            result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
            return
        }

        val barcode = zxingResult.toBarcode()

        if (isRequestedFormat(barcode.format, codeTypes) && filter(barcode)) {
            result(BarcodeResult.OnSuccess(barcode))
        } else {
            result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
        }
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(e))
    }
}
