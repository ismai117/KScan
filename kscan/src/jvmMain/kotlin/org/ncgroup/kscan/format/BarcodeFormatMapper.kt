package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat
import com.google.zxing.BarcodeFormat as ZxingFormat

internal object BarcodeFormatMapper {

    private val formats = FormatMap(
        mapOf(
            ZxingFormat.QR_CODE to BarcodeFormat.FORMAT_QR_CODE,
            ZxingFormat.CODE_128 to BarcodeFormat.FORMAT_CODE_128,
            ZxingFormat.CODE_39 to BarcodeFormat.FORMAT_CODE_39,
            ZxingFormat.CODE_93 to BarcodeFormat.FORMAT_CODE_93,
            ZxingFormat.CODABAR to BarcodeFormat.FORMAT_CODABAR,
            ZxingFormat.EAN_13 to BarcodeFormat.FORMAT_EAN_13,
            ZxingFormat.EAN_8 to BarcodeFormat.FORMAT_EAN_8,
            ZxingFormat.ITF to BarcodeFormat.FORMAT_ITF,
            ZxingFormat.UPC_A to BarcodeFormat.FORMAT_UPC_A,
            ZxingFormat.UPC_E to BarcodeFormat.FORMAT_UPC_E,
            ZxingFormat.PDF_417 to BarcodeFormat.FORMAT_PDF417,
            ZxingFormat.AZTEC to BarcodeFormat.FORMAT_AZTEC,
            ZxingFormat.DATA_MATRIX to BarcodeFormat.FORMAT_DATA_MATRIX,
        ),
    )

    fun toZxingFormats(appFormats: List<BarcodeFormat>): List<ZxingFormat> = formats.platformFormats(appFormats)

    fun toAppFormat(zxingFormat: ZxingFormat): BarcodeFormat = formats.appFormat(zxingFormat)
}
