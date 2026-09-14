package org.ncgroup.kscan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import org.ncgroup.kscan.format.firstMatching
import org.ncgroup.kscan.scanner.barcodeReaderOptions
import org.ncgroup.kscan.scanner.invertedLuminance
import org.ncgroup.kscan.scanner.toBarcode
import zxingcpp.BarcodeReader
import java.util.concurrent.Executors

// zxing-cpp decodes on the thread that calls it, and a photograph takes long
// enough that doing so on the caller's would drop frames. ML Kit's task API kept
// this off the main thread and answered on it, so that is what is kept: a caller
// scanning from a picker callback is not made to wait for the decoder.
private val decoders =
    Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "kscan-image-scan").apply { isDaemon = true }
    }

public actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val main = Handler(Looper.getMainLooper())

    decoders.execute {
        val outcome =
            try {
                decode(imageBytes, codeTypes, filter)
            } catch (e: Exception) {
                BarcodeResult.OnFailed(e)
            }

        main.post { result(outcome) }
    }
}

private fun decode(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
): BarcodeResult {
    // zxing-cpp reads eight bit luminance and 32 bit RGBA only, so the config is
    // asked for rather than left to whatever the encoded image suggests.
    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }

    val decoded =
        try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        } catch (e: Exception) {
            return BarcodeResult.OnFailed(Exception("Failed to decode image bytes", e))
        }
            ?: return BarcodeResult.OnFailed(Exception("Failed to decode image bytes"))

    val readable =
        decoded.takeIf { it.config == Bitmap.Config.ARGB_8888 }
            ?: decoded.copy(Bitmap.Config.ARGB_8888, false)
            ?: return BarcodeResult.OnFailed(Exception("Failed to decode image bytes"))

    val reader = BarcodeReader(barcodeReaderOptions(codeTypes))

    val found = reader.read(readable)
    found.match(codeTypes, filter)?.let { return BarcodeResult.OnSuccess(it) }

    // The same negative pass the camera makes, for the same reason: zxing-cpp
    // offers tryInvert only to the readers that allow reversed reflectance, so a
    // linear symbol printed light on dark is never tried without it.
    val negative = reader.read(readable.invertedLuminance())
    negative.match(codeTypes, filter)?.let { return BarcodeResult.OnSuccess(it) }

    return if (found.isEmpty() && negative.isEmpty()) {
        BarcodeResult.OnFailed(Exception("No barcode found in image"))
    } else {
        BarcodeResult.OnFailed(Exception("No matching barcode found in image"))
    }
}

private fun List<BarcodeReader.Result>.match(
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
): Barcode? = mapNotNull { it.toBarcode() }.firstMatching(codeTypes, filter)
