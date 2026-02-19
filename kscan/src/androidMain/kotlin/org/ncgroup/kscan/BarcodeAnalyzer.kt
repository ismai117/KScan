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
    private val getCamera: () -> Camera?,
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
                    val camera = getCamera()
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
            }
            .addOnCanceledListener {
                onCanceled()
            }
            .addOnCompleteListener {
                // CRITICAL: Always close the proxy after the final attempt
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun createInvertedInputImage(imageProxy: ImageProxy): InputImage {
        val mediaImage = imageProxy.image ?: throw IllegalArgumentException("Image is null")
        require(mediaImage.planes.isNotEmpty()) { "Image has no planes" }

        val width = mediaImage.width
        val height = mediaImage.height
        val yPixelCount = width * height
        val nv21Bytes = ByteArray(yPixelCount * 3 / 2)

        val yPlane = mediaImage.planes[0]
        val rowStride = yPlane.rowStride
        require(rowStride >= width) { "Invalid Y rowStride: $rowStride, width: $width" }

        val yBuffer = yPlane.buffer.duplicate()
        val rowBytes = ByteArray(width)

        // Bulk-read one row at a time, then invert into output (fewer ByteBuffer.get() calls)
        for (row in 0 until height) {
            yBuffer.position(row * rowStride)
            yBuffer.get(rowBytes, 0, width)

            val outBase = row * width
            for (col in 0 until width) {
                nv21Bytes[outBase + col] = (rowBytes[col].toInt() xor 0xFF).toByte()
            }
        }

        // Neutral chroma for grayscale in NV21 (VU interleaved)
        java.util.Arrays.fill(nv21Bytes, yPixelCount, nv21Bytes.size, 128.toByte())

        return InputImage.fromByteArray(
            nv21Bytes,
            width,
            height,
            imageProxy.imageInfo.rotationDegrees,
            InputImage.IMAGE_FORMAT_NV21,
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
