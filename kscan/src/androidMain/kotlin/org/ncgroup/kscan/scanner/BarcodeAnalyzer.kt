package org.ncgroup.kscan.scanner

import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.isRequestedFormat
import zxingcpp.BarcodeReader
import java.util.concurrent.Executor

/**
 * Decodes camera frames.
 *
 * zxing-cpp decodes on the calling thread, so this is bound to a background
 * executor and hands what it finds to [callbackExecutor]. Everything a caller
 * supplied runs there, which keeps [filter] and [onSuccess] on the thread they
 * have always been called on, and confines the decision state to it.
 */
internal class BarcodeAnalyzer(
    private val codeTypes: List<BarcodeFormat>,
    options: BarcodeReader.Options,
    private val callbackExecutor: Executor,
    private val onSuccess: (List<Barcode>) -> Unit,
    private val onFailed: (Exception) -> Unit,
    private val filter: (Barcode) -> Boolean,
) : ImageAnalysis.Analyzer {
    private val reader = BarcodeReader(options)
    private val repeated = RepeatedDetection()
    private val inverter = FrameInverter()

    // Written on the callback thread, read on the analysis thread.
    @Volatile
    private var hasSuccessfullyProcessedBarcode = false

    @Volatile
    private var closed = false

    private var emptyFrames = 0

    override fun analyze(imageProxy: ImageProxy) {
        if (closed || hasSuccessfullyProcessedBarcode) {
            imageProxy.close()
            return
        }

        val barcodes =
            try {
                imageProxy.use { decode(it) }
            } catch (e: Exception) {
                if (!closed) callbackExecutor.execute { onFailed(e) }
                return
            }

        if (barcodes.isNotEmpty()) {
            callbackExecutor.execute { report(barcodes) }
        }
    }

    private fun decode(imageProxy: ImageProxy): List<Barcode> {
        val found = requested(reader.read(imageProxy))
        if (found.isNotEmpty()) return found

        // Inverting costs a full-frame copy, so it is paced rather than run on
        // every frame that held no barcode.
        if (emptyFrames++ % INVERTED_SCAN_INTERVAL != 0) return emptyList()

        val negative =
            try {
                inverter.invert(imageProxy)
            } catch (_: Exception) {
                // A frame that cannot be inverted is dropped rather than reported:
                // this runs per frame, and the caller cannot act on it.
                return emptyList()
            }

        return requested(reader.read(negative, Rect(), imageProxy.imageInfo.rotationDegrees))
    }

    private fun requested(results: List<BarcodeReader.Result>): List<Barcode> = results
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
    }

    private companion object {
        // Invert and rescan one frame in this many that held no barcode.
        const val INVERTED_SCAN_INTERVAL = 4
    }
}
