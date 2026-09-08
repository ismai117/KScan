package org.ncgroup.kscan.benchmark

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
import org.ncgroup.kscan.benchmark.corpus.Jitter
import org.ncgroup.kscan.benchmark.engine.BitmapEngine
import org.ncgroup.kscan.benchmark.engine.MlKitEngine
import org.ncgroup.kscan.benchmark.engine.ZxingCppEngine
import org.ncgroup.kscan.benchmark.engine.toBitmap

/**
 * How long each decoder takes to read a scene, rather than whether it reads one
 * frame of it.
 *
 * A scanner sees about thirty frames a second and stops at the first one it
 * reads, so a single-frame read rate overstates what a user notices: a decoder
 * that reads three frames in ten still resolves the scene in a third of a second.
 * This replays every condition as a second of hand-held video and records the
 * frame each decoder first read it on.
 *
 * The jitter is deliberately small. See [Jitter].
 */
@RunWith(Parameterized::class)
class MultiFrameTest(
    private val group: String,
) {
    @Test
    fun `GIVEN a second of video THEN the frame each decoder first reads on is recorded`() {
        val samples = BarcodeCorpus.SAMPLES.filter { it.condition.group == group }

        assertTrue("No samples for group $group", samples.isNotEmpty())

        samples.forEach { sample ->
            val readOn = mutableMapOf<String, Int>()
            val seed = sample.id.hashCode().toLong()

            for (index in 0 until FRAMES) {
                if (readOn.size == ENGINES.size) break

                val frame = sample.render(Jitter.handHeld(seed, index))
                val bitmap = frame.toBitmap()
                val negative = ImageOps.invert(frame).toBitmap()

                try {
                    ENGINES
                        .filter { it.name !in readOn }
                        .forEach { engine ->
                            val decoded = engine.decodeOrNull(bitmap) ?: engine.decodeOrNull(negative)
                            if (decoded != null && sample.matches(decoded.text)) readOn[engine.name] = index + 1
                        }
                } finally {
                    bitmap.recycle()
                    negative.recycle()
                }
            }

            ENGINES.forEach { RESULTS += Scan(it.name, sample.format.name, sample.condition.name, group, readOn[it.name]) }
        }

        ENGINES.forEach { engine ->
            val scans = RESULTS.filter { it.engine == engine.name && it.group == group }
            Log.i(TAG, "$group / ${engine.name}: ${scans.count { it.readOn != null }}/${scans.size} scenes read")
        }
    }

    private fun BitmapEngine.decodeOrNull(bitmap: android.graphics.Bitmap) = runCatching { decode(bitmap) }.getOrNull()

    data class Scan(
        val engine: String,
        val format: String,
        val condition: String,
        val group: String,
        val readOn: Int?,
    )

    companion object {
        private const val TAG = "KScanBenchmark"

        /** One second at thirty frames a second. */
        private const val FRAMES = 30

        private val RESULTS = mutableListOf<Scan>()

        private val ENGINES: List<BitmapEngine> by lazy {
            listOf(MlKitEngine(), ZxingCppEngine("zxing-cpp", ZxingCppEngine.kscanOptions()))
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

            storage.openOutputFile("multi-frame.md").bufferedWriter().use { it.write(render(engines)) }
            storage.openOutputFile("multi-frame.csv").bufferedWriter().use { writer ->
                writer.write("engine,format,condition,group,read_on_frame\n")
                RESULTS.forEach { writer.write("${it.engine},${it.format},${it.condition},${it.group},${it.readOn ?: ""}\n") }
            }

            engines.forEach { engine ->
                val scans = RESULTS.filter { it.engine == engine }
                Log.i(TAG, "TOTAL $engine: ${scans.count { it.readOn != null }}/${scans.size} scenes read")
            }
        }

        private fun render(engines: List<String>): String {
            val builder = StringBuilder()

            builder.appendLine("# Frames to first read, over a second of hand-held video").appendLine()
            builder.appendLine("- **Device**: ${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}")
            builder.appendLine("- **Scenes**: ${BarcodeCorpus.SAMPLES.size}, replayed as $FRAMES frames each")
            builder.appendLine(
                "- **Jitter**: ${Jitter.ROTATION_SIGMA}' rotation, ${Jitter.TRANSLATION_SIGMA}px shift, " +
                    "${(Jitter.EXPOSURE_SIGMA * 100).toInt()}% exposure, ${Jitter.READ_NOISE_SIGMA} read noise, all one sigma",
            )
            builder.appendLine("- A scene counts as read when a decoder returns the expected payload on any frame.").appendLine()

            builder.append(rows("Every scene", engines) { listOf("all") })
            builder.append(rows("By group", engines) { RESULTS.map { scan -> scan.group }.distinct() })
            builder.append(rows("By condition", engines) { RESULTS.map { scan -> scan.condition }.distinct() })

            return builder.toString()
        }

        /** One row per key, with how many of its scenes each decoder read and how quickly. */
        private fun rows(
            heading: String,
            engines: List<String>,
            keys: () -> List<String>,
        ): String {
            val builder = StringBuilder()

            builder.appendLine("## $heading").appendLine()
            // Almost everything reads on the first frame, so the median says nothing;
            // the ninetieth percentile is where the slow scenes show up.
            builder.appendLine("| | ${engines.joinToString(" | ") { "$it read" }} | ${engines.joinToString(" | ") { "$it p90 frame" }} |")
            builder.appendLine("|---|" + "---:|".repeat(engines.size * 2))

            keys().forEach { key ->
                val forKey = RESULTS.filter { key == "all" || it.group == key || it.condition == key }
                val cells = engines.map { engine -> forKey.filter { it.engine == engine } }

                builder.append("| $key |")
                cells.forEach { scans ->
                    val read = scans.count { it.readOn != null }
                    builder.append(String.format(" %.1f%% |", if (scans.isEmpty()) 0.0 else read * 100.0 / scans.size))
                }
                cells.forEach { scans ->
                    val frames = scans.mapNotNull { it.readOn }.sorted()
                    builder.append(if (frames.isEmpty()) " - |" else " ${frames[(frames.size - 1) * 9 / 10]} |")
                }
                builder.appendLine()
            }

            return builder.appendLine().toString()
        }
    }
}
