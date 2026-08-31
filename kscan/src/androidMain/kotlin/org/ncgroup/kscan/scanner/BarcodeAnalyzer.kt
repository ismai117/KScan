package org.ncgroup.kscan.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.isRequestedFormat

internal class BarcodeAnalyzer(
    private val codeTypes: List<BarcodeFormat>,
    scannerOptions: BarcodeScannerOptions,
    private val onSuccess: (List<Barcode>) -> Unit,
    private val onFailed: (Exception) -> Unit,
    private val filter: (Barcode) -> Boolean,
) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(scannerOptions)
    private val repeated = RepeatedDetection()
    private val inverter = FrameInverter()
    private var hasSuccessfullyProcessedBarcode = false

    private var emptyFrames = 0

    // ML Kit finishes whatever it was already decoding, so without this a result
    // or a "detector is closed" failure could reach a caller that has left.
    private var closed = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (closed || hasSuccessfullyProcessedBarcode) {
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
                if (closed) {
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                val relevantBarcodes = requested(barcodes)
                if (relevantBarcodes.isNotEmpty()) {
                    report(relevantBarcodes)
                    imageProxy.close()
                } else if (emptyFrames++ % INVERTED_SCAN_INTERVAL == 0) {
                    // Inverting costs a full-frame copy, so it is paced rather than
                    // run on every frame.
                    scanInverted(imageProxy)
                } else {
                    imageProxy.close()
                }
            }
            .addOnFailureListener {
                if (!closed) onFailed(it)
                imageProxy.close()
            }
            .addOnCanceledListener {
                imageProxy.close()
            }
    }

    // A frame that cannot be inverted is dropped rather than reported: this runs
    // per frame, and the caller cannot act on it.
    private fun scanInverted(imageProxy: ImageProxy) {
        val invertedImage = try {
            inverter.invert(imageProxy)
        } catch (e: Exception) {
            imageProxy.close()
            return
        }

        scanner.process(invertedImage)
            .addOnSuccessListener { barcodes ->
                if (closed) {
                    imageProxy.close()
                    return@addOnSuccessListener
                }

                val relevantBarcodes = requested(barcodes)
                if (relevantBarcodes.isNotEmpty()) {
                    report(relevantBarcodes)
                }
            }
            .addOnFailureListener {
                if (!closed) onFailed(it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun requested(
        mlKitBarcodes: List<com.google.mlkit.vision.barcode.common.Barcode>,
    ): List<Barcode> = mlKitBarcodes
        .mapNotNull { it.toBarcode() }
        .filter { isRequestedFormat(it.format, codeTypes) }

    private fun report(barcodes: List<Barcode>) {
        if (closed || hasSuccessfullyProcessedBarcode) return

        for (barcode in barcodes) {
            if (!repeated.accept(barcode.data)) continue

            // Rejected by the caller: keep looking at the rest of the frame.
            if (!filter(barcode)) continue

            onSuccess(listOf(barcode))
            repeated.reset()
            hasSuccessfullyProcessedBarcode = true
            break
        }
    }

    fun close() {
        closed = true
        scanner.close()
    }

    private companion object {
        // Invert and rescan one frame in this many that held no barcode.
        const val INVERTED_SCAN_INTERVAL = 4
    }
}
