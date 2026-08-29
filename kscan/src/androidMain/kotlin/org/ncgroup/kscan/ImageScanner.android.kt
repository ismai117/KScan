package org.ncgroup.kscan

import android.graphics.BitmapFactory
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

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

    val inputImage = InputImage.fromBitmap(bitmap, 0)

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(BarcodeFormatMapper.toMlKitFormats(codeTypes))
        .build()

    val scanner = BarcodeScanning.getClient(options)

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            val matchingBarcode = barcodes
                .mapNotNull { mlKitBarcode ->
                    val displayValue = mlKitBarcode.displayValue ?: return@mapNotNull null
                    val appFormat = BarcodeFormatMapper.toAppFormat(mlKitBarcode.format)

                    if (!isRequestedFormat(appFormat, codeTypes)) return@mapNotNull null

                    Barcode(
                        data = displayValue,
                        format = appFormat.toString(),
                        rawBytes = mlKitBarcode.rawBytes ?: displayValue.encodeToByteArray(),
                    )
                }
                .firstOrNull(filter)

            if (matchingBarcode != null) {
                result(BarcodeResult.OnSuccess(matchingBarcode))
            } else if (barcodes.isEmpty()) {
                result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
            } else {
                result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
            }
        }
        .addOnFailureListener { exception ->
            result(BarcodeResult.OnFailed(exception))
        }
}
