package org.ncgroup.kscan

/**
 * Maps between app [BarcodeFormat] and BarcodeDetector Web API format strings.
 */
internal object BarcodeFormatMapper {

    private val WEB_TO_APP_FORMAT_MAP: Map<String, BarcodeFormat> = mapOf(
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
    )

    private val APP_TO_WEB_FORMAT_MAP: Map<BarcodeFormat, String> =
        WEB_TO_APP_FORMAT_MAP.entries.associateBy({ it.value }) { it.key }

    val allSupportedTypes: List<String> = WEB_TO_APP_FORMAT_MAP.keys.toList()

    fun toWebFormats(appFormats: List<BarcodeFormat>): List<String> {
        if (appFormats.isEmpty() || appFormats.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return allSupportedTypes
        }
        return appFormats.mapNotNull { APP_TO_WEB_FORMAT_MAP[it] }
    }

    fun toAppFormat(webFormat: String): BarcodeFormat = WEB_TO_APP_FORMAT_MAP[webFormat] ?: BarcodeFormat.TYPE_UNKNOWN

    fun isKnownFormat(webFormat: String): Boolean = WEB_TO_APP_FORMAT_MAP.containsKey(webFormat)
}
