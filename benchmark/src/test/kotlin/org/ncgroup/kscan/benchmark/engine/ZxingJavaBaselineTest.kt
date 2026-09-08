package org.ncgroup.kscan.benchmark.engine

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.ncgroup.kscan.benchmark.corpus.BarcodeCorpus
import org.ncgroup.kscan.benchmark.corpus.Frame
import org.ncgroup.kscan.benchmark.report.Observation
import org.ncgroup.kscan.benchmark.report.Outcome
import org.ncgroup.kscan.benchmark.report.Report
import org.ncgroup.kscan.benchmark.report.outcomeOf
import java.io.File

/**
 * The pure-Java ZXing decoder over the whole corpus.
 *
 * This is the second replacement the issue puts forward, and it is the only one of
 * the three that runs on a JVM, so it needs no emulator. Its numbers are also the
 * corpus's own sanity check: a clean frame that ZXing cannot read is a generator
 * bug, not a decoder result.
 */
class ZxingJavaBaselineTest {
    private val hints =
        mapOf<DecodeHintType, Any>(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "ISO-8859-1",
        )

    @Test
    fun `GIVEN a clean frame THEN every format decodes to its payload`() {
        val baseline = BarcodeCorpus.SAMPLES.filter { it.condition.name == "baseline" }

        val failures =
            baseline.mapNotNull { sample ->
                val decoded = decode(sample.render())
                if (sample.matches(decoded?.text)) null else "${sample.id} -> ${decoded?.text}"
            }

        assertEquals("Corpus frames that ZXing could not read cleanly", emptyList<String>(), failures)
    }

    @Test
    fun `GIVEN the whole corpus THEN the ZXing Java results are written out`() {
        val observations =
            BarcodeCorpus.SAMPLES.map { sample ->
                val frame = sample.render()

                val start = System.nanoTime()
                val decoded = decode(frame)
                val elapsed = (System.nanoTime() - start) / 1_000

                Observation(
                    engine = ENGINE,
                    format = sample.format.name,
                    condition = sample.condition.name,
                    group = sample.condition.group,
                    pixelsPerModule = sample.pixelsPerModule,
                    outcome = outcomeOf(sample, decoded?.text),
                    decodedFormat = decoded?.format,
                    decodedText = decoded?.text,
                    elapsedMicros = elapsed,
                )
            }

        val directory = File("build/reports/benchmark").apply { mkdirs() }
        File(directory, "zxing-java.md").writeText(
            Report.render(
                title = "ZXing Java over the KScan corpus",
                environment =
                mapOf(
                    "Decoder" to "com.google.zxing:core, TRY_HARDER, inverted and quarter-turn retries",
                    "Frames" to "${observations.size} (${BarcodeCorpus.SPECIMENS.size} formats)",
                    "Frame size" to "${BarcodeCorpus.FRAME_WIDTH}x${BarcodeCorpus.FRAME_HEIGHT}",
                ),
                engines = listOf(ENGINE),
                observations = observations,
            ),
        )
        File(directory, "zxing-java.csv").writeText(Report.csv(observations))

        println("Wrote ${directory.absolutePath}/zxing-java.md")
        println("Read correctly: ${observations.count { it.outcome == Outcome.HIT }}/${observations.size}")
    }

    private fun decode(frame: Frame): Decoded? {
        // The retries mirror the options KScan gives zxing-cpp, so the three
        // decoders are asked to work equally hard.
        val candidates =
            listOf(
                RGBLuminanceSource(frame.width, frame.height, frame.pixels),
                RGBLuminanceSource(frame.height, frame.width, quarterTurn(frame)),
            )

        candidates.forEach { source ->
            listOf(source, source.invert()).forEach { candidate ->
                val result =
                    runCatching {
                        MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(candidate)), hints)
                    }.getOrNull()

                if (result != null) return Decoded(result.barcodeFormat.name, result.text)
            }
        }

        return null
    }

    private fun quarterTurn(frame: Frame): IntArray {
        val rotated = IntArray(frame.pixels.size)

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                rotated[x * frame.height + (frame.height - 1 - y)] = frame[x, y]
            }
        }

        return rotated
    }

    private data class Decoded(
        val format: String,
        val text: String,
    )

    private companion object {
        const val ENGINE = "zxing-java"
    }
}
