package org.ncgroup.kscan

/**
 * Scans a barcode from an image provided as a byte array.
 *
 * This function allows scanning barcodes from static images (e.g., from gallery,
 * screenshots, or downloaded images) rather than from a live camera feed.
 *
 * @param imageBytes The image data as a byte array (e.g., PNG, JPEG).
 * @param codeTypes The barcode formats to scan for. Defaults to all formats.
 * @param filter Optional filter to accept or reject detected barcodes.
 * @param result Callback invoked with the scan result.
 *
 * Example usage:
 * ```kotlin
 * val imageBytes = selectedImage.readBytes()
 * scanImage(
 *     imageBytes = imageBytes,
 *     codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE)
 * ) { result ->
 *     when (result) {
 *         is BarcodeResult.OnSuccess -> println(result.barcode.data)
 *         is BarcodeResult.OnFailed -> println(result.exception.message)
 *         is BarcodeResult.OnCanceled -> { /* not applicable */ }
 *     }
 * }
 * ```
 */
expect fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat> = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
    filter: (Barcode) -> Boolean = { true },
    result: (BarcodeResult) -> Unit,
)
