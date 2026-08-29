package org.ncgroup.kscan

/**
 * True when [format] is one the caller asked for.
 *
 * Every platform decodes differently but decides this identically: an empty
 * [codeTypes], or one containing [BarcodeFormat.FORMAT_ALL_FORMATS], accepts any
 * recognised format; otherwise the format must be listed.
 */
internal fun isRequestedFormat(
    format: BarcodeFormat,
    codeTypes: List<BarcodeFormat>,
): Boolean {
    val hasAllFormats =
        codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)

    return if (hasAllFormats) format != BarcodeFormat.TYPE_UNKNOWN else format in codeTypes
}
