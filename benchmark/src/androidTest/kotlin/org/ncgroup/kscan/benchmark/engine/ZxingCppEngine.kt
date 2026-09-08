package org.ncgroup.kscan.benchmark.engine

import android.graphics.Bitmap
import zxingcpp.BarcodeReader

/** zxing-cpp with a named set of options, so several configurations can be compared side by side. */
class ZxingCppEngine(
    override val name: String,
    options: BarcodeReader.Options,
) : BitmapEngine {
    private val reader = BarcodeReader(options)

    override fun decode(bitmap: Bitmap): Decoded? {
        val result = reader.read(bitmap).firstOrNull { it.text != null } ?: return null

        return Decoded(FORMATS[result.format] ?: result.format.name, result.text!!)
    }

    companion object {
        /**
         * What KScan will ship: every format, and the three retries that stand in
         * for the frame inversion and rotation handling ML Kit does internally.
         */
        fun kscanOptions(): BarcodeReader.Options = BarcodeReader.Options(
            formats = emptySet(),
            tryHarder = true,
            tryRotate = true,
            tryInvert = true,
            tryDownscale = true,
            textMode = BarcodeReader.TextMode.PLAIN,
        )

        private val FORMATS =
            mapOf(
                BarcodeReader.Format.QR_CODE to "QR_CODE",
                BarcodeReader.Format.DATA_MATRIX to "DATA_MATRIX",
                BarcodeReader.Format.AZTEC to "AZTEC",
                BarcodeReader.Format.PDF_417 to "PDF_417",
                BarcodeReader.Format.CODE_128 to "CODE_128",
                BarcodeReader.Format.CODE_39 to "CODE_39",
                BarcodeReader.Format.CODE_93 to "CODE_93",
                BarcodeReader.Format.CODABAR to "CODABAR",
                BarcodeReader.Format.ITF to "ITF",
                BarcodeReader.Format.EAN_13 to "EAN_13",
                BarcodeReader.Format.EAN_8 to "EAN_8",
                BarcodeReader.Format.UPC_A to "UPC_A",
                BarcodeReader.Format.UPC_E to "UPC_E",
            )
    }
}
