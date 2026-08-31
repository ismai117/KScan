package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat
import platform.AVFoundation.AVMetadataObjectType
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCodabarCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeInterleaved2of5Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode

/**
 * Maps between app [BarcodeFormat] and AVFoundation metadata object types.
 */
internal object BarcodeFormatMapper {

    private val formats = FormatMap(
        mapOf(
            AVMetadataObjectTypeQRCode to BarcodeFormat.FORMAT_QR_CODE,
            AVMetadataObjectTypeEAN13Code to BarcodeFormat.FORMAT_EAN_13,
            AVMetadataObjectTypeEAN8Code to BarcodeFormat.FORMAT_EAN_8,
            AVMetadataObjectTypeCode128Code to BarcodeFormat.FORMAT_CODE_128,
            AVMetadataObjectTypeCode39Code to BarcodeFormat.FORMAT_CODE_39,
            AVMetadataObjectTypeCode93Code to BarcodeFormat.FORMAT_CODE_93,
            AVMetadataObjectTypeUPCECode to BarcodeFormat.FORMAT_UPC_E,
            AVMetadataObjectTypePDF417Code to BarcodeFormat.FORMAT_PDF417,
            AVMetadataObjectTypeAztecCode to BarcodeFormat.FORMAT_AZTEC,
            AVMetadataObjectTypeDataMatrixCode to BarcodeFormat.FORMAT_DATA_MATRIX,
            AVMetadataObjectTypeInterleaved2of5Code to BarcodeFormat.FORMAT_ITF,
            AVMetadataObjectTypeCodabarCode to BarcodeFormat.FORMAT_CODABAR,
        ),
    )

    val allSupportedTypes: List<AVMetadataObjectType> = formats.all

    fun toAvTypes(appFormats: List<BarcodeFormat>): List<AVMetadataObjectType> = formats.platformFormats(appFormats)

    fun toAppFormat(avType: AVMetadataObjectType): BarcodeFormat = formats.appFormat(avType)
}
