package org.ncgroup.kscan

import org.ncgroup.kscan.format.isRequestedFormat
import org.ncgroup.kscan.scanner.FrameDecoder
import org.ncgroup.kscan.scanner.toBarcode
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

        val zxingResult = FrameDecoder(zxingReader(codeTypes)).decode(bufferedImage)

        if (zxingResult == null) {
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
