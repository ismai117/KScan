package org.ncgroup.kscan

internal fun BarcodeFormat.toZxingFormat(): com.google.zxing.BarcodeFormat? {
    return when (this) {
        BarcodeFormat.FORMAT_CODE_128 -> com.google.zxing.BarcodeFormat.CODE_128
        BarcodeFormat.FORMAT_CODE_39 -> com.google.zxing.BarcodeFormat.CODE_39
        BarcodeFormat.FORMAT_CODE_93 -> com.google.zxing.BarcodeFormat.CODE_93
        BarcodeFormat.FORMAT_CODABAR -> com.google.zxing.BarcodeFormat.CODABAR
        BarcodeFormat.FORMAT_EAN_13 -> com.google.zxing.BarcodeFormat.EAN_13
        BarcodeFormat.FORMAT_EAN_8 -> com.google.zxing.BarcodeFormat.EAN_8
        BarcodeFormat.FORMAT_ITF -> com.google.zxing.BarcodeFormat.ITF
        BarcodeFormat.FORMAT_UPC_A -> com.google.zxing.BarcodeFormat.UPC_A
        BarcodeFormat.FORMAT_UPC_E -> com.google.zxing.BarcodeFormat.UPC_E
        BarcodeFormat.FORMAT_QR_CODE -> com.google.zxing.BarcodeFormat.QR_CODE
        BarcodeFormat.FORMAT_PDF417 -> com.google.zxing.BarcodeFormat.PDF_417
        BarcodeFormat.FORMAT_AZTEC -> com.google.zxing.BarcodeFormat.AZTEC
        BarcodeFormat.FORMAT_DATA_MATRIX -> com.google.zxing.BarcodeFormat.DATA_MATRIX
        else -> null
    }
}

internal fun com.google.zxing.BarcodeFormat.toKScanFormat(): BarcodeFormat {
    return when (this) {
        com.google.zxing.BarcodeFormat.CODE_128 -> BarcodeFormat.FORMAT_CODE_128
        com.google.zxing.BarcodeFormat.CODE_39 -> BarcodeFormat.FORMAT_CODE_39
        com.google.zxing.BarcodeFormat.CODE_93 -> BarcodeFormat.FORMAT_CODE_93
        com.google.zxing.BarcodeFormat.CODABAR -> BarcodeFormat.FORMAT_CODABAR
        com.google.zxing.BarcodeFormat.EAN_13 -> BarcodeFormat.FORMAT_EAN_13
        com.google.zxing.BarcodeFormat.EAN_8 -> BarcodeFormat.FORMAT_EAN_8
        com.google.zxing.BarcodeFormat.ITF -> BarcodeFormat.FORMAT_ITF
        com.google.zxing.BarcodeFormat.UPC_A -> BarcodeFormat.FORMAT_UPC_A
        com.google.zxing.BarcodeFormat.UPC_E -> BarcodeFormat.FORMAT_UPC_E
        com.google.zxing.BarcodeFormat.QR_CODE -> BarcodeFormat.FORMAT_QR_CODE
        com.google.zxing.BarcodeFormat.PDF_417 -> BarcodeFormat.FORMAT_PDF417
        com.google.zxing.BarcodeFormat.AZTEC -> BarcodeFormat.FORMAT_AZTEC
        com.google.zxing.BarcodeFormat.DATA_MATRIX -> BarcodeFormat.FORMAT_DATA_MATRIX
        else -> BarcodeFormat.TYPE_UNKNOWN
    }
}