package org.ncgroup.kscan

/**
 * An object that provides access to various barcode formats.
 *
 * This object contains constants for each supported barcode format,
 * which can be used to specify the desired format when scanning or generating barcodes.
 *
 * The available formats include:
 * - [FORMAT_CODE_128]
 * - [FORMAT_CODE_39]
 * - [FORMAT_CODE_93]
 * - [FORMAT_CODABAR]
 * - [FORMAT_EAN_13]
 * - [FORMAT_EAN_8]
 * - [FORMAT_ITF]
 * - [FORMAT_UPC_A]
 * - [FORMAT_UPC_E]
 * - [FORMAT_QR_CODE]
 * - [FORMAT_PDF417]
 * - [FORMAT_AZTEC]
 * - [FORMAT_DATA_MATRIX]
 * - [FORMAT_ALL_FORMATS] (allows scanning of all supported formats)
 *
 * Additionally, this object provides constants for different types of data
 * that can be encoded in barcodes:
 * - [TYPE_UNKNOWN]
 * - [TYPE_CONTACT_INFO]
 * - [TYPE_EMAIL]
 * - [TYPE_ISBN]
 * - [TYPE_PHONE]
 * - [TYPE_PRODUCT]
 * - [TYPE_SMS]
 * - [TYPE_TEXT]
 * - [TYPE_URL]
 * - [TYPE_WIFI]
 * - [TYPE_GEO]
 * - [TYPE_CALENDAR_EVENT]
 * - [TYPE_DRIVER_LICENSE]
 */
expect object BarcodeFormats {
    val FORMAT_CODE_128: BarcodeFormat
    val FORMAT_CODE_39: BarcodeFormat
    val FORMAT_CODE_93: BarcodeFormat
    val FORMAT_CODABAR: BarcodeFormat
    val FORMAT_EAN_13: BarcodeFormat
    val FORMAT_EAN_8: BarcodeFormat
    val FORMAT_ITF: BarcodeFormat
    val FORMAT_UPC_A: BarcodeFormat
    val FORMAT_UPC_E: BarcodeFormat
    val FORMAT_QR_CODE: BarcodeFormat
    val FORMAT_PDF417: BarcodeFormat
    val FORMAT_AZTEC: BarcodeFormat
    val FORMAT_DATA_MATRIX: BarcodeFormat
    val FORMAT_ALL_FORMATS: BarcodeFormat
    val TYPE_UNKNOWN: BarcodeFormat
    val TYPE_CONTACT_INFO: BarcodeFormat
    val TYPE_EMAIL: BarcodeFormat
    val TYPE_ISBN: BarcodeFormat
    val TYPE_PHONE: BarcodeFormat
    val TYPE_PRODUCT: BarcodeFormat
    val TYPE_SMS: BarcodeFormat
    val TYPE_TEXT: BarcodeFormat
    val TYPE_URL: BarcodeFormat
    val TYPE_WIFI: BarcodeFormat
    val TYPE_GEO: BarcodeFormat
    val TYPE_CALENDAR_EVENT: BarcodeFormat
    val TYPE_DRIVER_LICENSE: BarcodeFormat
}


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