package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat

/**
 * Maps between app [BarcodeFormat] and BarcodeDetector Web API format strings.
 */
internal object BarcodeFormatMapper {

    private val formats = FormatMap(
        mapOf(
            "qr_code" to BarcodeFormat.FORMAT_QR_CODE,
            "ean_13" to BarcodeFormat.FORMAT_EAN_13,
            "ean_8" to BarcodeFormat.FORMAT_EAN_8,
            "code_128" to BarcodeFormat.FORMAT_CODE_128,
            "code_39" to BarcodeFormat.FORMAT_CODE_39,
            "code_93" to BarcodeFormat.FORMAT_CODE_93,
            "codabar" to BarcodeFormat.FORMAT_CODABAR,
            "itf" to BarcodeFormat.FORMAT_ITF,
            "upc_a" to BarcodeFormat.FORMAT_UPC_A,
            "upc_e" to BarcodeFormat.FORMAT_UPC_E,
            "pdf417" to BarcodeFormat.FORMAT_PDF417,
            "aztec" to BarcodeFormat.FORMAT_AZTEC,
            "data_matrix" to BarcodeFormat.FORMAT_DATA_MATRIX,
        ),
    )

    fun toWebFormats(appFormats: List<BarcodeFormat>): List<String> = formats.platformFormats(appFormats)

    fun toAppFormat(webFormat: String): BarcodeFormat = formats.appFormat(webFormat)
}
