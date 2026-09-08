package org.ncgroup.kscan.scanner

import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.BarcodeFormatMapper
import zxingcpp.BarcodeReader

/**
 * The decoder settings the library scans with.
 *
 * The three retries are what close most of the gap to a machine-learning
 * detector on awkward frames, and the local-average binarizer is what carries
 * unevenly lit ones; the benchmark in `:benchmark` measures each of these
 * choices against the alternatives.
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
