package org.ncgroup.kscan.scanner

import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.format.BarcodeFormatMapper
import zxingcpp.BarcodeReader

internal fun BarcodeReader.Result.toBarcode(): Barcode? {
    val value = text ?: return null

    return Barcode(
        data = value,
        format = BarcodeFormatMapper.toAppFormat(format),
        rawBytes = bytes ?: value.encodeToByteArray(),
    )
}
