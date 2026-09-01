package org.ncgroup.kscan

import android.graphics.BitmapFactory
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.ncgroup.kscan.format.BarcodeFormatMapper
import org.ncgroup.kscan.format.firstMatching
import org.ncgroup.kscan.scanner.toBarcode

public actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val bitmap = try {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(Exception("Failed to decode image bytes", e)))
        return
    }

    if (bitmap == null) {
        result(BarcodeResult.OnFailed(Exception("Failed to decode image bytes")))
        return
    }

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(BarcodeFormatMapper.toMlKitFormats(codeTypes))
        .build()

    val scanner = BarcodeScanning.getClient(options)

    scanner.process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { barcodes ->
            val matching = barcodes.mapNotNull { it.toBarcode() }.firstMatching(codeTypes, filter)

            when {
                matching != null -> result(BarcodeResult.OnSuccess(matching))
                barcodes.isEmpty() -> result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
                else -> result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
            }
        }
        .addOnFailureListener { exception ->
            result(BarcodeResult.OnFailed(exception))
        }
        .addOnCompleteListener {
            scanner.close()
        }
}
