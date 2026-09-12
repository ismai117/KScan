package org.ncgroup.kscan.format

import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat

internal fun wantsEveryFormat(codeTypes: List<BarcodeFormat>): Boolean = codeTypes.isEmpty() || BarcodeFormat.FORMAT_ALL_FORMATS in codeTypes

/**
 * A symbology KScan has no name for is never reported, however it was asked for.
 *
 * A decoder's repertoire is wider than the thirteen formats the enum covers, and
 * naming [BarcodeFormat.TYPE_UNKNOWN] in `codeTypes` used to let everything past
 * that: it maps to no platform format, which the decoders read as no restriction
 * at all, and whatever they then found came back matching the request. A barcode
 * whose format is unknown tells a caller nothing it can act on either way.
 */
internal fun isRequestedFormat(
    format: BarcodeFormat,
    codeTypes: List<BarcodeFormat>,
): Boolean = format != BarcodeFormat.TYPE_UNKNOWN && (wantsEveryFormat(codeTypes) || format in codeTypes)

internal fun List<Barcode>.firstMatching(
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
): Barcode? = firstOrNull { isRequestedFormat(it.format, codeTypes) && filter(it) }

internal class FormatMap<T>(private val toApp: Map<T, BarcodeFormat>) {
    private val fromApp: Map<BarcodeFormat, T> =
        toApp.entries.associateBy({ it.value }) { it.key }

    val all: List<T> = toApp.keys.toList()

    fun platformFormat(format: BarcodeFormat): T? = fromApp[format]

    fun platformFormats(codeTypes: List<BarcodeFormat>): List<T> = if (wantsEveryFormat(codeTypes)) all else codeTypes.mapNotNull(fromApp::get)

    fun appFormat(format: T): BarcodeFormat = toApp[format] ?: BarcodeFormat.TYPE_UNKNOWN
}
