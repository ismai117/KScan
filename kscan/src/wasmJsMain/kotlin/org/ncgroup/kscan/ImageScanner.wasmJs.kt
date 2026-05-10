package org.ncgroup.kscan

actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    result(BarcodeResult.OnFailed(Exception("Image scanning is not yet supported on WASM")))
}
