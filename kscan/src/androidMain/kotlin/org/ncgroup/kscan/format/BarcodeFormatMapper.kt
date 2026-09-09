package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat
import zxingcpp.BarcodeReader

internal object BarcodeFormatMapper {

    private val formats = FormatMap(
        mapOf(
            BarcodeReader.Format.QR_CODE to BarcodeFormat.FORMAT_QR_CODE,
            BarcodeReader.Format.CODE_128 to BarcodeFormat.FORMAT_CODE_128,
            BarcodeReader.Format.CODE_39 to BarcodeFormat.FORMAT_CODE_39,
            BarcodeReader.Format.CODE_93 to BarcodeFormat.FORMAT_CODE_93,
            BarcodeReader.Format.CODABAR to BarcodeFormat.FORMAT_CODABAR,
            BarcodeReader.Format.DATA_MATRIX to BarcodeFormat.FORMAT_DATA_MATRIX,
            BarcodeReader.Format.EAN_13 to BarcodeFormat.FORMAT_EAN_13,
            BarcodeReader.Format.EAN_8 to BarcodeFormat.FORMAT_EAN_8,
            BarcodeReader.Format.ITF to BarcodeFormat.FORMAT_ITF,
            BarcodeReader.Format.UPC_A to BarcodeFormat.FORMAT_UPC_A,
            BarcodeReader.Format.UPC_E to BarcodeFormat.FORMAT_UPC_E,
            BarcodeReader.Format.PDF_417 to BarcodeFormat.FORMAT_PDF417,
            BarcodeReader.Format.AZTEC to BarcodeFormat.FORMAT_AZTEC,
        ),
    )

    // Named rather than left empty for every format: an empty set asks zxing-cpp
    // for every symbology it knows, including the ones KScan has no name for,
    // which would only be decoded to be reported as TYPE_UNKNOWN and dropped.
    fun toZxingCppFormats(appFormats: List<BarcodeFormat>): Set<BarcodeReader.Format> = formats.platformFormats(appFormats).toSet()

    fun toAppFormat(zxingCppFormat: BarcodeReader.Format): BarcodeFormat = formats.appFormat(zxingCppFormat)
}
