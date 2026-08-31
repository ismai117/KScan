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
import platform.Vision.VNBarcodeSymbologyCodabar
import platform.Vision.VNBarcodeSymbologyCode128
import platform.Vision.VNBarcodeSymbologyCode39
import platform.Vision.VNBarcodeSymbologyCode93
import platform.Vision.VNBarcodeSymbologyDataMatrix
import platform.Vision.VNBarcodeSymbologyEAN13
import platform.Vision.VNBarcodeSymbologyEAN8
import platform.Vision.VNBarcodeSymbologyI2of5
import platform.Vision.VNBarcodeSymbologyPDF417
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNBarcodeSymbologyUPCE
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRequest

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public actual fun scanImage(
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

        val matchingBarcode = observations
            .mapNotNull { observation ->
                val symbology = observation.symbology ?: return@mapNotNull null
                val appFormat = visionFormats.appFormat(symbology)

                if (!isRequestedFormat(appFormat, codeTypes)) return@mapNotNull null

                val payloadString = observation.payloadStringValue ?: return@mapNotNull null

                Barcode(
                    data = payloadString,
                    format = appFormat,
                    rawBytes = payloadString.encodeToByteArray(),
                )
            }
            .firstOrNull(filter)

        if (matchingBarcode != null) {
            result(BarcodeResult.OnSuccess(matchingBarcode))
        } else {
            result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
        }
    }

    // Vision rejects a symbology the OS release does not know, and Codabar only
    // arrived in iOS 15, so ask the request itself what it can be given.
    val supported = request.supportedSymbologiesAndReturnError(null).orEmpty()
    val symbologies = visionFormats.platformFormats(codeTypes).filter { it in supported }

    if (symbologies.isNotEmpty()) {
        request.setSymbologies(symbologies)
    }

    try {
        handler.performRequests(listOf(request), null)
    } catch (e: Exception) {
        result(BarcodeResult.OnFailed(Exception("Failed to perform barcode detection: ${e.message}")))
    }
}

/**
 * Still images decode through Vision, which names its formats differently from the
 * AVFoundation metadata types the live camera reports.
 */
private val visionFormats = FormatMap(
    mapOf(
        VNBarcodeSymbologyQR to BarcodeFormat.FORMAT_QR_CODE,
        VNBarcodeSymbologyEAN13 to BarcodeFormat.FORMAT_EAN_13,
        VNBarcodeSymbologyEAN8 to BarcodeFormat.FORMAT_EAN_8,
        VNBarcodeSymbologyCode128 to BarcodeFormat.FORMAT_CODE_128,
        VNBarcodeSymbologyCode39 to BarcodeFormat.FORMAT_CODE_39,
        VNBarcodeSymbologyCode93 to BarcodeFormat.FORMAT_CODE_93,
        VNBarcodeSymbologyUPCE to BarcodeFormat.FORMAT_UPC_E,
        VNBarcodeSymbologyPDF417 to BarcodeFormat.FORMAT_PDF417,
        VNBarcodeSymbologyAztec to BarcodeFormat.FORMAT_AZTEC,
        VNBarcodeSymbologyDataMatrix to BarcodeFormat.FORMAT_DATA_MATRIX,
        VNBarcodeSymbologyCodabar to BarcodeFormat.FORMAT_CODABAR,
        VNBarcodeSymbologyI2of5 to BarcodeFormat.FORMAT_ITF,
    ),
)
