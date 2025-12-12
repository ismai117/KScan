package org.ncgroup.kscan

import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODABAR
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UNKNOWN
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E
import com.google.mlkit.vision.common.InputImage

/**
 * Analyzes images for barcodes using ML Kit.
 *
 * This class implements [ImageAnalysis.Analyzer] to process camera frames.
 * It uses ML Kit's Barcode Scanning API to detect and decode barcodes.
 *
 * It features:
 * - **Configurable Barcode Types**: Scans for specific barcode formats defined by `codeTypes`.
 * - **Zoom Suggestion**: Utilizes ML Kit's zoom suggestion feature to prompt the user to zoom if a barcode is detected but is too small. The zoom is handled automatically if the camera supports it.
 * - **Duplicate Filtering**: To ensure accuracy and prevent multiple triggers for the same barcode, a barcode must be detected twice in quick succession before it's considered successfully processed.
 * - **Single Success Processing**: Once a barcode is successfully processed (detected twice), further analysis is stopped to prevent redundant callbacks.
 * - **Callbacks**:
 *     - `onSuccess`: Called when a barcode is successfully detected and meets the criteria.
 *     - `onFailed`: Called if an error occurs during the barcode scanning process.
 *     - `onCanceled`: Called if the barcode scanning task is canceled.
 *
 * The analyzer maps ML Kit's barcode formats to a custom `BarcodeFormat` enum for application-specific use.
 *
 * @property camera The [Camera] instance, used for zoom control. Can be null if zoom control is not needed or available.
 * @property codeTypes A list of [BarcodeFormat] enums specifying which barcode types to scan for. If empty or contains `BarcodeFormat.FORMAT_ALL_FORMATS`, all supported formats are scanned.
 * @property onSuccess A callback function that is invoked when a barcode is successfully detected and validated. It receives a list containing the single detected [Barcode].
 * @property onFailed A callback function that is invoked when an error occurs during the image analysis or barcode scanning process. It receives the [Exception] that occurred.
 * @property onCanceled A callback function that is invoked if the barcode scanning task is canceled.
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
            .setBarcodeFormats(getMLKitBarcodeFormats(codeTypes))
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
    private var hasSuccessfullyProcessedBarcode = false //

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
                val appSpecificFormat = mlKitFormatToAppFormat(mlKitBarcode.format)
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
            return MLKIT_TO_APP_FORMAT_MAP.containsKey(mlKitBarcode.format)
        }
        val appFormat = mlKitFormatToAppFormat(mlKitBarcode.format)
        return codeTypes.contains(appFormat)
    }

    companion object {
        private val APP_TO_MLKIT_FORMAT_MAP: Map<BarcodeFormat, Int> =
            mapOf(
                BarcodeFormat.FORMAT_QR_CODE to FORMAT_QR_CODE,
                BarcodeFormat.FORMAT_CODE_128 to FORMAT_CODE_128,
                BarcodeFormat.FORMAT_CODE_39 to FORMAT_CODE_39,
                BarcodeFormat.FORMAT_CODE_93 to FORMAT_CODE_93,
                BarcodeFormat.FORMAT_CODABAR to FORMAT_CODABAR,
                BarcodeFormat.FORMAT_DATA_MATRIX to FORMAT_DATA_MATRIX,
                BarcodeFormat.FORMAT_EAN_13 to FORMAT_EAN_13,
                BarcodeFormat.FORMAT_EAN_8 to FORMAT_EAN_8,
                BarcodeFormat.FORMAT_ITF to FORMAT_ITF,
                BarcodeFormat.FORMAT_UPC_A to FORMAT_UPC_A,
                BarcodeFormat.FORMAT_UPC_E to FORMAT_UPC_E,
                BarcodeFormat.FORMAT_PDF417 to FORMAT_PDF417,
                BarcodeFormat.FORMAT_AZTEC to FORMAT_AZTEC,
            )

        private val MLKIT_TO_APP_FORMAT_MAP: Map<Int, BarcodeFormat> =
            APP_TO_MLKIT_FORMAT_MAP.entries.associateBy({ it.value }) { it.key }
                .plus(FORMAT_UNKNOWN to BarcodeFormat.TYPE_UNKNOWN)

        fun getMLKitBarcodeFormats(appFormats: List<BarcodeFormat>): Int {
            if (appFormats.isEmpty() || appFormats.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
                return FORMAT_ALL_FORMATS
            }

            return appFormats
                .mapNotNull { APP_TO_MLKIT_FORMAT_MAP[it] }
                .distinct()
                .fold(0) { acc, formatInt -> acc or formatInt }
                .let { if (it == 0) FORMAT_ALL_FORMATS else it }
        }

        fun mlKitFormatToAppFormat(mlKitFormat: Int): BarcodeFormat {
            return MLKIT_TO_APP_FORMAT_MAP[mlKitFormat] ?: BarcodeFormat.TYPE_UNKNOWN
        }
    }
}
