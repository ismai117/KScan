package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat

/**
 * True when [codeTypes] singles out no format and so accepts every one the
 * platform can decode: an empty list, or one naming [BarcodeFormat.FORMAT_ALL_FORMATS].
 */
internal fun wantsEveryFormat(codeTypes: List<BarcodeFormat>): Boolean = codeTypes.isEmpty() || BarcodeFormat.FORMAT_ALL_FORMATS in codeTypes

/**
 * True when [format] is one the caller asked for.
 *
 * Every platform decodes differently but decides this identically.
 */
internal fun isRequestedFormat(
    format: BarcodeFormat,
    codeTypes: List<BarcodeFormat>,
): Boolean = if (wantsEveryFormat(codeTypes)) {
    format != BarcodeFormat.TYPE_UNKNOWN
} else {
    format in codeTypes
}

/**
 * A two-way mapping between [BarcodeFormat] and the format type one decoder speaks.
 *
 * Each decoder names its formats differently — AVFoundation metadata types, Vision
 * symbologies, BarcodeDetector strings, ML Kit flags — but every one is asked the
 * same questions, so only the table itself belongs to the platform.
 */
internal class FormatMap<T>(private val toApp: Map<T, BarcodeFormat>) {
    private val fromApp: Map<BarcodeFormat, T> =
        toApp.entries.associateBy({ it.value }) { it.key }

    /** Every format in the table. */
    val all: List<T> = toApp.keys.toList()

    /** What [format] is called here, or `null` when this decoder cannot read it. */
    fun platformFormat(format: BarcodeFormat): T? = fromApp[format]

    /** The formats [codeTypes] selects, which is [all] when it selects none. */
    fun platformFormats(codeTypes: List<BarcodeFormat>): List<T> = if (wantsEveryFormat(codeTypes)) all else codeTypes.mapNotNull(fromApp::get)

    /** The [BarcodeFormat] [format] stands for, or [BarcodeFormat.TYPE_UNKNOWN]. */
    fun appFormat(format: T): BarcodeFormat = toApp[format] ?: BarcodeFormat.TYPE_UNKNOWN
}
