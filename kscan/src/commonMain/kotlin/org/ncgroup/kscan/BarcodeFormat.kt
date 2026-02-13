package org.ncgroup.kscan

/**
 * Supported barcode formats for scanning.
 *
 * This enum defines the barcode formats that can be recognized by the scanner.
 * Use these values with [ScannerView]'s `codeTypes` parameter to specify which
 * formats to scan for.
 *
 * **Barcode Formats (physical encoding):**
 * - 1D: [FORMAT_CODE_128], [FORMAT_CODE_39], [FORMAT_CODE_93], [FORMAT_CODABAR],
 *   [FORMAT_EAN_13], [FORMAT_EAN_8], [FORMAT_ITF], [FORMAT_UPC_A], [FORMAT_UPC_E]
 * - 2D: [FORMAT_QR_CODE], [FORMAT_PDF417], [FORMAT_AZTEC], [FORMAT_DATA_MATRIX]
 * - [FORMAT_ALL_FORMATS] - scan for all supported formats
 *
 * **Value Types (data content):**
 * - [TYPE_UNKNOWN], [TYPE_CONTACT_INFO], [TYPE_EMAIL], [TYPE_ISBN], [TYPE_PHONE],
 *   [TYPE_PRODUCT], [TYPE_SMS], [TYPE_TEXT], [TYPE_URL], [TYPE_WIFI], [TYPE_GEO],
 *   [TYPE_CALENDAR_EVENT], [TYPE_DRIVER_LICENSE]
 *
 * Example usage:
 * ```kotlin
 * ScannerView(
 *     codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.FORMAT_EAN_13),
 *     result = { result -> /* handle result */ }
 * )
 * ```
 */
enum class BarcodeFormat {
    FORMAT_CODE_128,
    FORMAT_CODE_39,
    FORMAT_CODE_93,
    FORMAT_CODABAR,
    FORMAT_EAN_13,
    FORMAT_EAN_8,
    FORMAT_ITF,
    FORMAT_UPC_A,
    FORMAT_UPC_E,
    FORMAT_QR_CODE,
    FORMAT_PDF417,
    FORMAT_AZTEC,
    FORMAT_DATA_MATRIX,
    FORMAT_ALL_FORMATS,
    TYPE_UNKNOWN,
    TYPE_CONTACT_INFO,
    TYPE_EMAIL,
    TYPE_ISBN,
    TYPE_PHONE,
    TYPE_PRODUCT,
    TYPE_SMS,
    TYPE_TEXT,
    TYPE_URL,
    TYPE_WIFI,
    TYPE_GEO,
    TYPE_CALENDAR_EVENT,
    TYPE_DRIVER_LICENSE,
}