package org.ncgroup.kscan.scanner

import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.format.BarcodeFormatMapper
import com.google.mlkit.vision.barcode.common.Barcode as MlKitBarcode

internal fun MlKitBarcode.toBarcode(): Barcode? {
    val value = displayValue ?: return null

    return Barcode(
        data = value,
        format = BarcodeFormatMapper.toAppFormat(format),
        rawBytes = rawBytes ?: value.encodeToByteArray(),
    )
}
