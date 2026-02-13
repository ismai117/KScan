package org.ncgroup.kscan

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

/**
 * Maps between app [BarcodeFormat] and ML Kit barcode format integers.
 */
internal object BarcodeFormatMapper {

    private val APP_TO_MLKIT_FORMAT_MAP: Map<BarcodeFormat, Int> = mapOf(
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

    fun toMlKitFormats(appFormats: List<BarcodeFormat>): Int {
        if (appFormats.isEmpty() || appFormats.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return FORMAT_ALL_FORMATS
        }

        return appFormats
            .mapNotNull { APP_TO_MLKIT_FORMAT_MAP[it] }
            .distinct()
            .fold(0) { acc, formatInt -> acc or formatInt }
            .let { if (it == 0) FORMAT_ALL_FORMATS else it }
    }

    fun toAppFormat(mlKitFormat: Int): BarcodeFormat {
        return MLKIT_TO_APP_FORMAT_MAP[mlKitFormat] ?: BarcodeFormat.TYPE_UNKNOWN
    }

    fun isKnownFormat(mlKitFormat: Int): Boolean {
        return MLKIT_TO_APP_FORMAT_MAP.containsKey(mlKitFormat)
    }
}
