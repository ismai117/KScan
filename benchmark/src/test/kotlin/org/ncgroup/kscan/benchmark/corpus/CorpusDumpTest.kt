package org.ncgroup.kscan.benchmark.corpus

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Writes the corpus out as PNGs so it can be looked at rather than taken on trust.
 *
 * Off by default, since it costs far more than the rest of the suite and produces
 * a directory nothing else reads:
 *
 * ```
 * ./gradlew :benchmark:testDebugUnitTest -Dkscan.corpus.dump=true
 * ```
 *
 * The frames land under `build/reports/benchmark/corpus`, one directory per
 * condition, alongside an `index.html` that lays them out for browsing.
 */
class CorpusDumpTest {
    @Test
    fun `GIVEN the dump is asked for THEN every sample is written as a PNG`() {
        assumeTrue(
            "Set -Dkscan.corpus.dump=true to write the corpus out",
            System.getProperty("kscan.corpus.dump") == "true",
        )

        val directory = File(System.getProperty("kscan.corpus.dir") ?: "build/reports/benchmark/corpus")
        directory.mkdirs()

        BarcodeCorpus.SAMPLES.forEach { sample ->
            val frame = sample.render()
            val image = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB)
            image.setRGB(0, 0, frame.width, frame.height, frame.pixels, 0, frame.width)

            val file = File(directory, "${sample.condition.name}/${sample.format.name}.png")
            file.parentFile.mkdirs()
            ImageIO.write(image, "png", file)
        }

        File(directory, "index.html").writeText(contactSheet())

        println("Wrote ${BarcodeCorpus.SAMPLES.size} frames to ${directory.absolutePath}")
    }

    private fun contactSheet(): String {
        val builder = StringBuilder()

        builder.appendLine(
            """
            <!doctype html><meta charset="utf-8"><title>KScan barcode corpus</title>
            <style>
              body { font: 14px system-ui, sans-serif; margin: 24px; background: #111; color: #eee; }
              h2 { margin: 32px 0 4px; font-size: 16px; }
              p { margin: 0 0 12px; color: #999; }
              .row { display: flex; flex-wrap: wrap; gap: 8px; }
              figure { margin: 0; width: 260px; }
              img { width: 100%; display: block; background: #000; }
              figcaption { font-size: 11px; color: #aaa; padding-top: 2px; }
            </style>
            <h1>KScan barcode corpus</h1>
            """.trimIndent(),
        )

        Conditions.ALL.forEach { condition ->
            val samples = BarcodeCorpus.SAMPLES.filter { it.condition.name == condition.name }

            builder.appendLine("<h2>${condition.name}</h2>")
            builder.appendLine("<p>group: ${condition.group}, symbol spans ${(condition.sizeFraction * 100).toInt()}% of the frame</p>")
            builder.appendLine("<div class=\"row\">")
            samples.forEach {
                builder.appendLine(
                    "<figure><a href=\"${condition.name}/${it.format.name}.png\">" +
                        "<img src=\"${condition.name}/${it.format.name}.png\" loading=\"lazy\"></a>" +
                        "<figcaption>${it.format.name}, ${it.pixelsPerModule} px/module</figcaption></figure>",
                )
            }
            builder.appendLine("</div>")
        }

        return builder.toString()
    }
}
