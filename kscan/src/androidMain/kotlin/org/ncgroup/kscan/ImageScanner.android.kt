package org.ncgroup.kscan

import android.graphics.BitmapFactory
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val bitmap = try {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(Exception("Failed to decode image: ${e.message}")))
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
            val hasAllFormats = codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)

            val matchingBarcode = barcodes.firstOrNull { mlKitBarcode ->
                val isFormatMatch = if (hasAllFormats) {
                    BarcodeFormatMapper.isKnownFormat(mlKitBarcode.format)
                } else {
                    val appFormat = BarcodeFormatMapper.toAppFormat(mlKitBarcode.format)
                    codeTypes.contains(appFormat)
                }

                if (!isFormatMatch) return@firstOrNull false

                val displayValue = mlKitBarcode.displayValue ?: return@firstOrNull false
                val rawBytes = mlKitBarcode.rawBytes ?: displayValue.encodeToByteArray()
                val appFormat = BarcodeFormatMapper.toAppFormat(mlKitBarcode.format)

                val barcode = Barcode(
                    data = displayValue,
                    format = appFormat.toString(),
                    rawBytes = rawBytes,
                )

                filter(barcode)
            }

            if (matchingBarcode != null) {
                val displayValue = matchingBarcode.displayValue!!
                val rawBytes = matchingBarcode.rawBytes ?: displayValue.encodeToByteArray()
                val appFormat = BarcodeFormatMapper.toAppFormat(matchingBarcode.format)

                val barcode = Barcode(
                    data = displayValue,
                    format = appFormat.toString(),
                    rawBytes = rawBytes,
                )
                result(BarcodeResult.OnSuccess(barcode))
            } else {
                result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
            }
        }
        .addOnFailureListener { exception ->
            result(BarcodeResult.OnFailed(exception))
        }
}
