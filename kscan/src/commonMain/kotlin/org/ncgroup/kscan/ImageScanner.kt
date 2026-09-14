package org.ncgroup.kscan

/**
 * Scans a still image for a barcode.
 *
 * Android and web decode away from the calling thread and answer on the main one,
 * so this returns before [result] is called. iOS and desktop decode on the thread
 * that calls them, so keep a large image off the main one there.
 *
 * @param imageBytes The image, in any format the platform decodes.
 * @param codeTypes The formats to scan for.
 * @param filter Called with each decoded barcode; returning `false` rejects it.
 * @param result Called with the outcome. [BarcodeResult.OnFailed] when nothing matched.
 */
public expect fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat> = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
    filter: (Barcode) -> Boolean = { true },
    result: (BarcodeResult) -> Unit,
)
