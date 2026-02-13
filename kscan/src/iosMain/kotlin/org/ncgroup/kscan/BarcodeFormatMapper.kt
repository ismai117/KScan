package org.ncgroup.kscan

import platform.AVFoundation.AVMetadataObjectType
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode

/**
 * Maps between app [BarcodeFormat] and AVFoundation metadata object types.
 */
internal object BarcodeFormatMapper {

    private val AV_TO_APP_FORMAT_MAP: Map<AVMetadataObjectType, BarcodeFormat> = mapOf(
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
    )

    private val APP_TO_AV_FORMAT_MAP: Map<BarcodeFormat, AVMetadataObjectType> =
        AV_TO_APP_FORMAT_MAP.entries.associateBy({ it.value }) { it.key }

    val allSupportedTypes: List<AVMetadataObjectType> = AV_TO_APP_FORMAT_MAP.keys.toList()

    fun toAvTypes(appFormats: List<BarcodeFormat>): List<AVMetadataObjectType> {
        if (appFormats.isEmpty() || appFormats.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return allSupportedTypes
        }
        return appFormats.mapNotNull { APP_TO_AV_FORMAT_MAP[it] }
    }

    fun toAppFormat(avType: AVMetadataObjectType): BarcodeFormat {
        return AV_TO_APP_FORMAT_MAP[avType] ?: BarcodeFormat.TYPE_UNKNOWN
    }

    fun isKnownFormat(avType: AVMetadataObjectType): Boolean {
        return AV_TO_APP_FORMAT_MAP.containsKey(avType)
    }
}
