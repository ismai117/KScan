package org.ncgroup.kscan

import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.common.InputImage

/**
 * Analyzes camera frames for barcodes using ML Kit.
 *
 * Features duplicate filtering (barcode must be detected twice) and auto-zoom suggestions.
 */
class BarcodeAnalyzer(
    private val camera: Camera?,
    private val codeTypes: List<BarcodeFormat>,
    private val onSuccess: (List<Barcode>) -> Unit,
    private val onFailed: (Exception) -> Unit,
    private val filter: (Barcode) -> Boolean,
    private val onCanceled: () -> Unit,
) : ImageAnalysis.Analyzer {
    private val scannerOptions =
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(BarcodeFormatMapper.toMlKitFormats(codeTypes))
            .setZoomSuggestionOptions(
                ZoomSuggestionOptions.Builder { zoomRatio ->
                    val maxZoomRatio =
                        (camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1.0f)
                            .coerceAtMost(5.0f)
                    if (zoomRatio <= maxZoomRatio) {
                        camera?.cameraControl?.setZoomRatio(zoomRatio)
                        true
                    } else {
                        false
                    }
                }.setMaxSupportedZoomRatio(5.0f).build(),
            )
            .build()

    private val scanner = BarcodeScanning.getClient(scannerOptions)
    private val barcodesDetected = mutableMapOf<String, Int>()
    private var hasSuccessfullyProcessedBarcode = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (hasSuccessfullyProcessedBarcode) {
            imageProxy.close()
            return
        }

        val mediaImage =
            imageProxy.image ?: run {
                imageProxy.close()
                return
            }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val relevantBarcodes = barcodes.filter { isRequestedFormat(it) }
                if (relevantBarcodes.isNotEmpty()) {
                    processFoundBarcodes(relevantBarcodes)
                    imageProxy.close()
                } else {
                    // If no barcodes found, try scanning the inverted image
                    scanInverted(imageProxy)
                }
            }
            .addOnFailureListener {
                onFailed(it)
                imageProxy.close()
            }
            .addOnCanceledListener {
                onCanceled()
                imageProxy.close()
            }
    }

    private fun scanInverted(imageProxy: ImageProxy) {
        val invertedImage = try {
            createInvertedInputImage(imageProxy)
        } catch (e: Exception) {
            // Conversion failed, clean up and exit
            imageProxy.close()
            return
        }

        scanner.process(invertedImage)
            .addOnSuccessListener { barcodes ->
                val relevantBarcodes = barcodes.filter { isRequestedFormat(it) }
                if (relevantBarcodes.isNotEmpty()) {
                    processFoundBarcodes(relevantBarcodes)
                }
            }
            .addOnFailureListener {
                onFailed(it)
                imageProxy.close()
            }
            .addOnCanceledListener {
                onCanceled()
                imageProxy.close()
            }
            .addOnCompleteListener {
                // CRITICAL: Always close the proxy after the final attempt
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun createInvertedInputImage(imageProxy: ImageProxy): InputImage {
        val mediaImage = imageProxy.image ?: throw IllegalArgumentException("Image is null")

        // Extract the luminance plane
        val yPlane = mediaImage.planes[0]
        val yBuffer = yPlane.buffer
        val ySize = yBuffer.remaining()
        val yBytes = ByteArray(ySize)
        yBuffer.get(yBytes)

        // This turns the Black background (low values) into White (high values)
        // and the Silver dots (high values) into Black (low values).
        for (i in yBytes.indices) {
            yBytes[i] = (255 - (yBytes[i].toInt() and 0xFF)).toByte()
        }

        // ML Kit fromByteArray requires NV21 format (Y + interleaved UV).
        // Since we only care about luminance for barcodes, we can fake the UV data with grey.
        val width = mediaImage.width
        val height = mediaImage.height
        val nv21Size = width * height * 3 / 2 // Standard NV21 size calculation
        val nv21Bytes = ByteArray(nv21Size)

        // Copy our inverted Y bytes to the start
        System.arraycopy(yBytes, 0, nv21Bytes, 0, ySize)

        // Fill the UV section with 127 (neutral grey) to avoid color noise interference
        java.util.Arrays.fill(nv21Bytes, ySize, nv21Bytes.size, 127.toByte())

        return InputImage.fromByteArray(
            nv21Bytes,
            width,
            height,
            imageProxy.imageInfo.rotationDegrees,
            InputImage.IMAGE_FORMAT_NV21
        )
    }

    private fun processFoundBarcodes(mlKitBarcodes: List<com.google.mlkit.vision.barcode.common.Barcode>) {
        if (hasSuccessfullyProcessedBarcode) return

        for (mlKitBarcode in mlKitBarcodes) {
            val displayValue = mlKitBarcode.displayValue ?: continue
            val rawBytes = mlKitBarcode.rawBytes ?: displayValue.encodeToByteArray()

            barcodesDetected[displayValue] = (barcodesDetected[displayValue] ?: 0) + 1
            if ((barcodesDetected[displayValue] ?: 0) >= 2) {
                val appSpecificFormat = BarcodeFormatMapper.toAppFormat(mlKitBarcode.format)
                val detectedAppBarcode =
                    Barcode(
                        data = displayValue,
                        format = appSpecificFormat.toString(),
                        rawBytes = rawBytes,
                    )

                if (!filter(detectedAppBarcode)) return

                onSuccess(listOf(detectedAppBarcode))
                barcodesDetected.clear()
                hasSuccessfullyProcessedBarcode = true
                break
            }
        }
    }

    private fun isRequestedFormat(mlKitBarcode: com.google.mlkit.vision.barcode.common.Barcode): Boolean {
        if (codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return BarcodeFormatMapper.isKnownFormat(mlKitBarcode.format)
        }
        val appFormat = BarcodeFormatMapper.toAppFormat(mlKitBarcode.format)
        return codeTypes.contains(appFormat)
    }
}
