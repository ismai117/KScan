package org.ncgroup.kscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.ncgroup.kscan.format.firstMatching
import org.ncgroup.kscan.scanner.barcodeReaderOptions
import org.ncgroup.kscan.scanner.toBarcode
import zxingcpp.BarcodeReader

public actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    // zxing-cpp reads eight bit luminance and 32 bit RGBA only, so the config is
    // asked for rather than left to whatever the encoded image suggests.
    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }

    val bitmap = try {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(Exception("Failed to decode image bytes", e)))
        return
    }

    if (bitmap == null) {
        result(BarcodeResult.OnFailed(Exception("Failed to decode image bytes")))
        return
    }

    val readable = bitmap.takeIf { it.config == Bitmap.Config.ARGB_8888 }
        ?: bitmap.copy(Bitmap.Config.ARGB_8888, false)

    if (readable == null) {
        result(BarcodeResult.OnFailed(Exception("Failed to decode image bytes")))
        return
    }

    val results = try {
        BarcodeReader(barcodeReaderOptions(codeTypes)).read(readable)
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(e))
        return
    }

    val matching = results.mapNotNull { it.toBarcode() }.firstMatching(codeTypes, filter)

    when {
        matching != null -> result(BarcodeResult.OnSuccess(matching))
        results.isEmpty() -> result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
        else -> result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
    }
}
