package org.ncgroup.kscan.benchmark

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.test.services.storage.TestStorage
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.ncgroup.kscan.benchmark.corpus.BarcodeCorpus
import org.ncgroup.kscan.benchmark.corpus.Conditions
import org.ncgroup.kscan.benchmark.corpus.ImageOps
import org.ncgroup.kscan.benchmark.engine.BitmapEngine
import org.ncgroup.kscan.benchmark.engine.MlKitEngine
import org.ncgroup.kscan.benchmark.engine.ZxingCppEngine
import org.ncgroup.kscan.benchmark.engine.toBitmap
import org.ncgroup.kscan.benchmark.report.Observation
import org.ncgroup.kscan.benchmark.report.Report
import org.ncgroup.kscan.benchmark.report.Tally
import org.ncgroup.kscan.benchmark.report.outcomeOf
import zxingcpp.BarcodeReader

/**
 * ML Kit against zxing-cpp over the generated corpus.
 *
 * Both decoders need an Android runtime, so this is the one part of the
 * comparison that cannot run on a JVM. It is split by condition group so that a
 * run reports progress rather than going quiet for several minutes, and so that
 * one group can be run on its own while iterating.
 *
 * Every engine is driven the way KScan's analyzer drives its decoder: the frame
 * first, then its negative if nothing came back. Neither library reads a
 * light-on-dark linear symbol without that second pass, so leaving it out would
 * measure something the library does not do.
 */
@RunWith(Parameterized::class)
class DecoderComparisonTest(
    private val group: String,
) {
    @Test
    fun `GIVEN a condition group THEN every decoder is measured over every format`() {
        val samples = BarcodeCorpus.SAMPLES.filter { it.condition.group == group }

        assertTrue("No samples for group $group", samples.isNotEmpty())

        samples.forEach { sample ->
            val frame = sample.render()
            val bitmap = frame.toBitmap()
            val negative = ImageOps.invert(frame).toBitmap()

            try {
                ENGINES.forEach { engine ->
                    val start = System.nanoTime()
                    val decoded = engine.decodeOrNull(bitmap) ?: engine.decodeOrNull(negative)
                    val elapsed = (System.nanoTime() - start) / 1_000

                    OBSERVATIONS +=
                        Observation(
                            engine = engine.name,
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
            } finally {
                bitmap.recycle()
                negative.recycle()
            }
        }

        ENGINES.forEach { engine ->
            val tally = Tally.of(OBSERVATIONS.filter { it.engine == engine.name && it.group == group })
            Log.i(TAG, "$group / ${engine.name}: ${tally.hits}/${tally.total} read, ${tally.misreads} misread")
        }
    }

    // A decoder throwing on a frame is a miss, not a failed run.
    private fun BitmapEngine.decodeOrNull(bitmap: Bitmap) = runCatching { decode(bitmap) }.getOrNull()

    companion object {
        private const val TAG = "KScanBenchmark"

        private val OBSERVATIONS = mutableListOf<Observation>()

        /**
         * ML Kit as KScan runs it today, then the zxing-cpp configurations worth
         * choosing between. `tryDenoise` is left out: it reaches the native side
         * but changed no outcome anywhere in the corpus.
         */
        private val ENGINES: List<BitmapEngine> by lazy {
            listOf(
                MlKitEngine(),
                ZxingCppEngine("zxing-cpp", ZxingCppEngine.kscanOptions()),
                ZxingCppEngine(
                    "zxing-cpp/global-hist",
                    ZxingCppEngine.kscanOptions().copy(binarizer = BarcodeReader.Binarizer.GLOBAL_HISTOGRAM),
                ),
                ZxingCppEngine(
                    "zxing-cpp/1-line",
                    ZxingCppEngine.kscanOptions().copy(minLineCount = 1),
                ),
                ZxingCppEngine(
                    "zxing-cpp/no-retries",
                    ZxingCppEngine.kscanOptions().copy(
                        tryHarder = false,
                        tryRotate = false,
                        tryInvert = false,
                        tryDownscale = false,
                    ),
                ),
            )
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun groups(): List<String> = Conditions.ALL.map { it.group }.distinct()

        @JvmStatic
        @AfterClass
        fun writeReport() {
            ENGINES.forEach { it.close() }

            val engines = ENGINES.map { it.name }
            val storage = TestStorage()

            storage.openOutputFile("decoders.md").bufferedWriter().use { writer ->
                writer.write(
                    Report.render(
                        title = "ML Kit against zxing-cpp over the KScan corpus",
                        environment =
                        mapOf(
                            "Device" to "${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}, ${Build.SUPPORTED_ABIS.first()}",
                            "Frames" to "${BarcodeCorpus.SAMPLES.size} (${BarcodeCorpus.SPECIMENS.size} formats x ${Conditions.ALL.size} conditions)",
                            "Frame size" to "${BarcodeCorpus.FRAME_WIDTH}x${BarcodeCorpus.FRAME_HEIGHT}, ARGB_8888",
                            "ML Kit" to "barcode-scanning, FORMAT_ALL_FORMATS",
                            "zxing-cpp" to "all formats, tryHarder, tryRotate, tryInvert, tryDownscale, TextMode.PLAIN",
                            "Both" to "the frame, then its negative if the frame gave nothing",
                        ),
                        engines = engines,
                        observations = OBSERVATIONS,
                    ),
                )
            }

            storage.openOutputFile("decoders.csv").bufferedWriter().use { it.write(Report.csv(OBSERVATIONS)) }

            engines.forEach { engine ->
                val tally = Tally.of(OBSERVATIONS.filter { it.engine == engine })
                Log.i(TAG, "TOTAL $engine: ${tally.hits}/${tally.total} read, ${tally.misreads} misread")
            }
        }
    }
}
