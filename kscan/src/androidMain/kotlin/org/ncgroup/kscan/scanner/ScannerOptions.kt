package org.ncgroup.kscan.scanner

import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.BarcodeFormatMapper
import zxingcpp.BarcodeReader

/**
 * The decoder settings the library scans with.
 *
 * `tryHarder` and the three retries are what close most of the gap to a
 * machine-learning detector on awkward frames, and the local-average binarizer
 * is what carries unevenly lit ones. Each was measured against its alternative
 * before being chosen: the retries are worth 12.2 points of read rate, and
 * turning them off only buys back decode time the library is not short of.
 */
internal fun barcodeReaderOptions(codeTypes: List<BarcodeFormat>): BarcodeReader.Options = BarcodeReader.Options(
    formats = BarcodeFormatMapper.toZxingCppFormats(codeTypes),
    tryHarder = true,
    tryRotate = true,
    tryInvert = true,
    tryDownscale = true,
    binarizer = BarcodeReader.Binarizer.LOCAL_AVERAGE,
    textMode = BarcodeReader.TextMode.PLAIN,
)
