package org.ncgroup.kscan.benchmark.corpus

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

/** A payload that is valid for [format], including its check digits where the symbology has them. */
data class Specimen(
    val format: BarcodeFormat,
    val payload: String,
    /**
     * Other spellings of this same symbol.
     *
     * A UPC-A is the EAN-13 that carries a leading zero, and a UPC-E stands for
     * the UPC-A it expands to; decoders differ over which of those they hand
     * back. Listing them keeps a naming convention from being scored as a
     * decoder reading the barcode wrongly.
     */
    val alsoAccepted: Set<String> = emptySet(),
) {
    fun accepts(text: String): Boolean = normalise(text) in (alsoAccepted + payload).map(::normalise)

    /** Decoders also disagree over whether Codabar's start and stop characters are part of the payload. */
    private fun normalise(text: String): String = if (format == BarcodeFormat.CODABAR) text.trim('A', 'B', 'C', 'D', 'a', 'b', 'c', 'd') else text
}

/** One rendered frame: a [Specimen] as seen under a [Condition]. */
class Sample(
    val specimen: Specimen,
    val condition: Condition,
) {
    val format: BarcodeFormat get() = specimen.format
    val payload: String get() = specimen.payload
    val id: String get() = "${format.name}/${condition.name}"

    private val matrix: BitMatrix by lazy { BarcodeCorpus.encode(specimen) }

    /** How many frame pixels one module of the symbol spans, the real measure of "distance". */
    val pixelsPerModule: Int by lazy { BarcodeCorpus.pixelsPerModule(matrix, condition.sizeFraction) }

    /** Whether [text] is the payload this sample was drawn from. */
    fun matches(text: String?): Boolean = text != null && specimen.accepts(text)

    fun render(): Frame {
        val scale = pixelsPerModule
        val symbol = BarcodeCorpus.rasterise(matrix, scale, condition.foreground, condition.background)
        val placed = BarcodeCorpus.centre(symbol, condition.background)

        return condition.degrade(placed)
    }
}

/**
 * The images both decoders are measured on.
 *
 * Everything is generated rather than checked in, so the corpus is reproducible
 * from source and reviewable as code instead of as several hundred PNGs.
 */
object BarcodeCorpus {
    const val FRAME_WIDTH: Int = 1280
    const val FRAME_HEIGHT: Int = 720

    /** How tall a linear symbol is rendered, in modules, before scaling. */
    private const val LINEAR_MODULE_ROWS = 30

    private val TWO_DIMENSIONAL =
        setOf(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.AZTEC,
            BarcodeFormat.PDF_417,
        )

    /** The thirteen formats KScan exposes, each with a payload its symbology accepts. */
    val SPECIMENS: List<Specimen> =
        listOf(
            Specimen(BarcodeFormat.QR_CODE, "https://github.com/ismai117/KScan"),
            Specimen(BarcodeFormat.DATA_MATRIX, "KScan-DataMatrix-0001"),
            Specimen(BarcodeFormat.AZTEC, "KScan-Aztec-0001"),
            Specimen(BarcodeFormat.PDF_417, "KScan-PDF417-0001"),
            Specimen(BarcodeFormat.CODE_128, "KScan-128-0001"),
            Specimen(BarcodeFormat.CODE_39, "KSCAN-39-0001"),
            Specimen(BarcodeFormat.CODE_93, "KSCAN-93-0001"),
            Specimen(BarcodeFormat.CODABAR, "A1234567890B"),
            Specimen(BarcodeFormat.ITF, "1234567890"),
            Specimen(BarcodeFormat.EAN_13, "5901234123457"),
            Specimen(BarcodeFormat.EAN_8, "96385074"),
            Specimen(BarcodeFormat.UPC_A, "036000291452", alsoAccepted = setOf("0036000291452")),
            Specimen(BarcodeFormat.UPC_E, "01234565", alsoAccepted = setOf("012345000065", "0012345000065")),
        )

    val SAMPLES: List<Sample> by lazy {
        SPECIMENS.flatMap { specimen -> Conditions.ALL.map { Sample(specimen, it) } }
    }

    internal fun encode(specimen: Specimen): BitMatrix {
        // The margin is dropped here because centring the symbol in a frame many
        // times its size already leaves a far larger quiet zone than the spec asks
        // for, and a margin baked into the matrix would distort the module count
        // the distance conditions are scaled from.
        val hints = mapOf(EncodeHintType.MARGIN to 0)
        val requestedHeight = if (specimen.format in TWO_DIMENSIONAL) 1 else LINEAR_MODULE_ROWS

        // Asking for one pixel of width makes every writer fall back to its own
        // minimum, which is the symbol at exactly one pixel per module.
        return MultiFormatWriter().encode(specimen.payload, specimen.format, 1, requestedHeight, hints)
    }

    internal fun pixelsPerModule(
        matrix: BitMatrix,
        sizeFraction: Double,
    ): Int {
        // The symbol takes that share of both dimensions, so a square code is
        // bounded by the frame's short side rather than silently pinned to it.
        val byWidth = (FRAME_WIDTH * sizeFraction / matrix.width).toInt()
        val byHeight = (FRAME_HEIGHT * sizeFraction / matrix.height).toInt()

        return minOf(byWidth, byHeight).coerceAtLeast(1)
    }

    internal fun rasterise(
        matrix: BitMatrix,
        scale: Int,
        foreground: Int,
        background: Int,
    ): Frame {
        val width = matrix.width * scale
        val height = matrix.height * scale
        val pixels = IntArray(width * height)

        for (row in 0 until matrix.height) {
            for (column in 0 until matrix.width) {
                val colour = if (matrix.get(column, row)) foreground else background
                for (dy in 0 until scale) {
                    val base = (row * scale + dy) * width + column * scale
                    java.util.Arrays.fill(pixels, base, base + scale, colour)
                }
            }
        }

        return Frame(width, height, pixels)
    }

    internal fun centre(
        symbol: Frame,
        background: Int,
    ): Frame {
        val frame = Frame.filled(FRAME_WIDTH, FRAME_HEIGHT, background)
        val left = (FRAME_WIDTH - symbol.width) / 2
        val top = (FRAME_HEIGHT - symbol.height) / 2

        require(left >= 0 && top >= 0) {
            "Symbol ${symbol.width}x${symbol.height} does not fit in ${FRAME_WIDTH}x$FRAME_HEIGHT"
        }

        for (row in 0 until symbol.height) {
            symbol.pixels.copyInto(
                destination = frame.pixels,
                destinationOffset = (top + row) * FRAME_WIDTH + left,
                startIndex = row * symbol.width,
                endIndex = (row + 1) * symbol.width,
            )
        }

        return frame
    }
}
