package org.ncgroup.kscan.benchmark.engine

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/** ML Kit as KScan configures it today: every format, defaults otherwise. */
class MlKitEngine : BitmapEngine {
    override val name: String = "mlkit"

    private val scanner =
        BarcodeScanning.getClient(
            BarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build(),
        )

    override fun decode(bitmap: Bitmap): Decoded? {
        val barcodes = Tasks.await(scanner.process(InputImage.fromBitmap(bitmap, 0)))
        val barcode = barcodes.firstOrNull { it.displayValue != null } ?: return null

        return Decoded(FORMATS[barcode.format] ?: "UNKNOWN(${barcode.format})", barcode.displayValue!!)
    }

    override fun close() {
        scanner.close()
    }

    private companion object {
        /** Named after the ZXing formats the corpus is generated from, so the columns line up. */
        val FORMATS =
            mapOf(
                Barcode.FORMAT_QR_CODE to "QR_CODE",
                Barcode.FORMAT_DATA_MATRIX to "DATA_MATRIX",
                Barcode.FORMAT_AZTEC to "AZTEC",
                Barcode.FORMAT_PDF417 to "PDF_417",
                Barcode.FORMAT_CODE_128 to "CODE_128",
                Barcode.FORMAT_CODE_39 to "CODE_39",
                Barcode.FORMAT_CODE_93 to "CODE_93",
                Barcode.FORMAT_CODABAR to "CODABAR",
                Barcode.FORMAT_ITF to "ITF",
                Barcode.FORMAT_EAN_13 to "EAN_13",
                Barcode.FORMAT_EAN_8 to "EAN_8",
                Barcode.FORMAT_UPC_A to "UPC_A",
                Barcode.FORMAT_UPC_E to "UPC_E",
            )
    }
}
