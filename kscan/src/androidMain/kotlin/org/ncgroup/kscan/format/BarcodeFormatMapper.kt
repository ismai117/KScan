package org.ncgroup.kscan.format

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
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E
import org.ncgroup.kscan.BarcodeFormat

internal object BarcodeFormatMapper {

    private val formats = FormatMap(
        mapOf(
            FORMAT_QR_CODE to BarcodeFormat.FORMAT_QR_CODE,
            FORMAT_CODE_128 to BarcodeFormat.FORMAT_CODE_128,
            FORMAT_CODE_39 to BarcodeFormat.FORMAT_CODE_39,
            FORMAT_CODE_93 to BarcodeFormat.FORMAT_CODE_93,
            FORMAT_CODABAR to BarcodeFormat.FORMAT_CODABAR,
            FORMAT_DATA_MATRIX to BarcodeFormat.FORMAT_DATA_MATRIX,
            FORMAT_EAN_13 to BarcodeFormat.FORMAT_EAN_13,
            FORMAT_EAN_8 to BarcodeFormat.FORMAT_EAN_8,
            FORMAT_ITF to BarcodeFormat.FORMAT_ITF,
            FORMAT_UPC_A to BarcodeFormat.FORMAT_UPC_A,
            FORMAT_UPC_E to BarcodeFormat.FORMAT_UPC_E,
            FORMAT_PDF417 to BarcodeFormat.FORMAT_PDF417,
            FORMAT_AZTEC to BarcodeFormat.FORMAT_AZTEC,
        ),
    )

    fun toMlKitFormats(appFormats: List<BarcodeFormat>): Int {
        if (wantsEveryFormat(appFormats)) return FORMAT_ALL_FORMATS

        return appFormats
            .mapNotNull(formats::platformFormat)
            .distinct()
            .fold(0) { acc, formatInt -> acc or formatInt }
            .let { if (it == 0) FORMAT_ALL_FORMATS else it }
    }

    fun toAppFormat(mlKitFormat: Int): BarcodeFormat = formats.appFormat(mlKitFormat)
}
