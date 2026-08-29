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
internal class BarcodeAnalyzer(
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
                            .coerceAtMost(MAX_ZOOM_RATIO)
                    if (zoomRatio <= maxZoomRatio) {
                        camera?.cameraControl?.setZoomRatio(zoomRatio)
                        true
                    } else {
                        false
                    }
                }.setMaxSupportedZoomRatio(MAX_ZOOM_RATIO).build(),
            )
            .build()

    private val scanner = BarcodeScanning.getClient(scannerOptions)
    private val repeated = RepeatedDetection()
    private var hasSuccessfullyProcessedBarcode = false

    /** Counts frames that held no barcode, to pace the inverted rescan. */
    private var emptyFrames = 0

    /**
     * Reused across frames. Inverting allocates roughly 1.5 bytes per pixel, which
     * at 1080p is about 3 MB, and the analyzer only ever inverts one frame at a
     * time: ImageAnalysis does not deliver the next frame until the current proxy
     * is closed, which happens after the inverted scan completes.
     */
    private var invertedBuffer: ByteArray? = null

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
                val relevantBarcodes = barcodes.filter { isRequested(it) }
                if (relevantBarcodes.isNotEmpty()) {
                    processFoundBarcodes(relevantBarcodes)
                    imageProxy.close()
                } else if (emptyFrames++ % INVERTED_SCAN_INTERVAL == 0) {
                    // Inverted (light-on-dark) codes need a second pass, which ML Kit
                    // will not do itself. Inverting costs a full-frame copy, so it is
                    // paced rather than run on every frame.
                    scanInverted(imageProxy)
                } else {
                    imageProxy.close()
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

    // A frame that cannot be inverted is dropped rather than reported: this runs
    // per frame, and the caller cannot act on it.
    private fun scanInverted(imageProxy: ImageProxy) {
        val invertedImage = try {
            createInvertedInputImage(imageProxy)
        } catch (e: Exception) {
            imageProxy.close()
            return
        }

        scanner.process(invertedImage)
            .addOnSuccessListener { barcodes ->
                val relevantBarcodes = barcodes.filter { isRequested(it) }
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
        val nv21Size = yPixelCount * 3 / 2
        val nv21Bytes = invertedBuffer?.takeIf { it.size == nv21Size }
            ?: ByteArray(nv21Size).also { invertedBuffer = it }

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

            if (repeated.accept(displayValue)) {
                val detectedAppBarcode =
                    Barcode(
                        data = displayValue,
                        format = BarcodeFormatMapper.toAppFormat(mlKitBarcode.format).toString(),
                        rawBytes = mlKitBarcode.rawBytes ?: displayValue.encodeToByteArray(),
                    )

                // Rejected by the caller: keep looking at the rest of the frame.
                if (!filter(detectedAppBarcode)) continue

                onSuccess(listOf(detectedAppBarcode))
                repeated.reset()
                hasSuccessfullyProcessedBarcode = true
                break
            }
        }
    }

    private fun isRequested(
        mlKitBarcode: com.google.mlkit.vision.barcode.common.Barcode,
    ): Boolean = isRequestedFormat(BarcodeFormatMapper.toAppFormat(mlKitBarcode.format), codeTypes)

    /** Releases the ML Kit detector, which holds native resources until closed. */
    fun close() {
        scanner.close()
    }

    private companion object {
        /** Invert and rescan one frame in this many that held no barcode. */
        const val INVERTED_SCAN_INTERVAL = 4
    }
}
