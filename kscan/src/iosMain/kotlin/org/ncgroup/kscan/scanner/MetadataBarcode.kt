package org.ncgroup.kscan.scanner

import kotlinx.cinterop.ExperimentalForeignApi
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.format.BarcodeFormatMapper
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.descriptor
import platform.CoreImage.CIQRCodeDescriptor

@OptIn(ExperimentalForeignApi::class)
internal fun AVMetadataMachineReadableCodeObject.toBarcode(): Barcode {
    val bytes = rawBytes()

    return Barcode(
        // A null byte would truncate the string wherever it is displayed.
        data = buildString {
            for (byte in bytes) {
                val char = (byte.toInt() and 0xFF).toChar()
                append(if (char == '\u0000') ' ' else char)
            }
        },
        format = BarcodeFormatMapper.toAppFormat(type),
        rawBytes = bytes,
    )
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("CAST_NEVER_SUCCEEDS")
private fun AVMetadataMachineReadableCodeObject.rawBytes(): ByteArray {
    if (type == AVMetadataObjectTypeQRCode) {
        (descriptor as? CIQRCodeDescriptor)
            ?.let { QRCodePayloadParser.extractRawBytes(it) }
            ?.let { return it }
    }

    return stringToRawBytes(stringValue ?: "")
}
