package org.ncgroup.kscan.format

import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat

internal fun wantsEveryFormat(codeTypes: List<BarcodeFormat>): Boolean = codeTypes.isEmpty() || BarcodeFormat.FORMAT_ALL_FORMATS in codeTypes

internal fun isRequestedFormat(
    format: BarcodeFormat,
    codeTypes: List<BarcodeFormat>,
): Boolean = if (wantsEveryFormat(codeTypes)) {
    format != BarcodeFormat.TYPE_UNKNOWN
} else {
    format in codeTypes
}

internal fun List<Barcode>.firstMatching(
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
): Barcode? = firstOrNull { isRequestedFormat(it.format, codeTypes) && filter(it) }

// Only the table belongs to a platform; every decoder is asked the same questions.
internal class FormatMap<T>(private val toApp: Map<T, BarcodeFormat>) {
    private val fromApp: Map<BarcodeFormat, T> =
        toApp.entries.associateBy({ it.value }) { it.key }

    val all: List<T> = toApp.keys.toList()

    fun platformFormat(format: BarcodeFormat): T? = fromApp[format]

    fun platformFormats(codeTypes: List<BarcodeFormat>): List<T> = if (wantsEveryFormat(codeTypes)) all else codeTypes.mapNotNull(fromApp::get)

    fun appFormat(format: T): BarcodeFormat = toApp[format] ?: BarcodeFormat.TYPE_UNKNOWN
}
