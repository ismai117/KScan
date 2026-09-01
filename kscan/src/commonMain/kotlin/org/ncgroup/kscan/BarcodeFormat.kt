package org.ncgroup.kscan

/**
 * The formats [ScannerView] and [scanImage] can be asked to scan for.
 *
 * [FORMAT_ALL_FORMATS] selects every format the platform supports.
 * [TYPE_UNKNOWN] is reported for a format the platform decoder does not map.
 */
public enum class BarcodeFormat {
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
}
