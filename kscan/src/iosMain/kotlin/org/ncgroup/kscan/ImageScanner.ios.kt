package org.ncgroup.kscan

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGImageRef
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNBarcodeSymbologyAztec
import platform.Vision.VNBarcodeSymbologyCode128
import platform.Vision.VNBarcodeSymbologyCode39
import platform.Vision.VNBarcodeSymbologyCode93
import platform.Vision.VNBarcodeSymbologyDataMatrix
import platform.Vision.VNBarcodeSymbologyEAN13
import platform.Vision.VNBarcodeSymbologyEAN8
import platform.Vision.VNBarcodeSymbologyPDF417
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNBarcodeSymbologyUPCE
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRequest

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val nsData = imageBytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
    }

    val uiImage = UIImage.imageWithData(nsData)
    if (uiImage == null) {
        result(BarcodeResult.OnFailed(Exception("Failed to decode image bytes")))
        return
    }

    val cgImage: CGImageRef = uiImage.CGImage ?: run {
        result(BarcodeResult.OnFailed(Exception("Failed to get CGImage from UIImage")))
        return
    }

    val handler = VNImageRequestHandler(cgImage, mapOf<Any?, Any?>())

    val request = VNDetectBarcodesRequest { request: VNRequest?, error: NSError? ->
        if (error != null) {
            result(BarcodeResult.OnFailed(Exception(error.localizedDescription)))
            return@VNDetectBarcodesRequest
        }

        val observations = request?.results?.filterIsInstance<VNBarcodeObservation>()
        if (observations.isNullOrEmpty()) {
            result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
            return@VNDetectBarcodesRequest
        }

        val hasAllFormats = codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)

        val matchingObservation = observations.firstOrNull { observation ->
            val symbology = observation.symbology ?: return@firstOrNull false
            val appFormat = symbologyToAppFormat(symbology)

            val isFormatMatch = if (hasAllFormats) {
                appFormat != BarcodeFormat.TYPE_UNKNOWN
            } else {
                codeTypes.contains(appFormat)
            }

            if (!isFormatMatch) return@firstOrNull false

            val payloadString = observation.payloadStringValue ?: return@firstOrNull false
            val rawBytes = payloadString.encodeToByteArray()

            val barcode = Barcode(
                data = payloadString,
                format = appFormat.toString(),
                rawBytes = rawBytes,
            )

            filter(barcode)
        }

        if (matchingObservation != null) {
            val payloadString = matchingObservation.payloadStringValue!!
            val appFormat = symbologyToAppFormat(matchingObservation.symbology ?: "")
            val rawBytes = payloadString.encodeToByteArray()

            val barcode = Barcode(
                data = payloadString,
                format = appFormat.toString(),
                rawBytes = rawBytes,
            )
            result(BarcodeResult.OnSuccess(barcode))
        } else {
            result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
        }
    }

    // Set symbologies to detect
    val symbologies = toVisionSymbologies(codeTypes)
    if (symbologies.isNotEmpty()) {
        request.setSymbologies(symbologies)
    }

    try {
        handler.performRequests(listOf(request), null)
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(Exception("Failed to perform barcode detection: ${e.message}")))
    }
}

private fun symbologyToAppFormat(symbology: String): BarcodeFormat {
    return when (symbology) {
        VNBarcodeSymbologyQR -> BarcodeFormat.FORMAT_QR_CODE
        VNBarcodeSymbologyEAN13 -> BarcodeFormat.FORMAT_EAN_13
        VNBarcodeSymbologyEAN8 -> BarcodeFormat.FORMAT_EAN_8
        VNBarcodeSymbologyCode128 -> BarcodeFormat.FORMAT_CODE_128
        VNBarcodeSymbologyCode39 -> BarcodeFormat.FORMAT_CODE_39
        VNBarcodeSymbologyCode93 -> BarcodeFormat.FORMAT_CODE_93
        VNBarcodeSymbologyUPCE -> BarcodeFormat.FORMAT_UPC_E
        VNBarcodeSymbologyPDF417 -> BarcodeFormat.FORMAT_PDF417
        VNBarcodeSymbologyAztec -> BarcodeFormat.FORMAT_AZTEC
        VNBarcodeSymbologyDataMatrix -> BarcodeFormat.FORMAT_DATA_MATRIX
        else -> BarcodeFormat.TYPE_UNKNOWN
    }
}

private fun toVisionSymbologies(appFormats: List<BarcodeFormat>): List<String> {
    if (appFormats.isEmpty() || appFormats.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
        return listOfNotNull(
            VNBarcodeSymbologyQR,
            VNBarcodeSymbologyEAN13,
            VNBarcodeSymbologyEAN8,
            VNBarcodeSymbologyCode128,
            VNBarcodeSymbologyCode39,
            VNBarcodeSymbologyCode93,
            VNBarcodeSymbologyUPCE,
            VNBarcodeSymbologyPDF417,
            VNBarcodeSymbologyAztec,
            VNBarcodeSymbologyDataMatrix,
        )
    }

    return appFormats.mapNotNull { format ->
        when (format) {
            BarcodeFormat.FORMAT_QR_CODE -> VNBarcodeSymbologyQR
            BarcodeFormat.FORMAT_EAN_13 -> VNBarcodeSymbologyEAN13
            BarcodeFormat.FORMAT_EAN_8 -> VNBarcodeSymbologyEAN8
            BarcodeFormat.FORMAT_CODE_128 -> VNBarcodeSymbologyCode128
            BarcodeFormat.FORMAT_CODE_39 -> VNBarcodeSymbologyCode39
            BarcodeFormat.FORMAT_CODE_93 -> VNBarcodeSymbologyCode93
            BarcodeFormat.FORMAT_UPC_E -> VNBarcodeSymbologyUPCE
            BarcodeFormat.FORMAT_PDF417 -> VNBarcodeSymbologyPDF417
            BarcodeFormat.FORMAT_AZTEC -> VNBarcodeSymbologyAztec
            BarcodeFormat.FORMAT_DATA_MATRIX -> VNBarcodeSymbologyDataMatrix
            else -> null
        }
    }
}
