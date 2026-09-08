package org.ncgroup.kscan.benchmark

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Test
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.benchmark.corpus.BarcodeCorpus
import org.ncgroup.kscan.benchmark.corpus.Sample
import org.ncgroup.kscan.benchmark.engine.toBitmap
import org.ncgroup.kscan.scanImage
import java.io.ByteArrayOutputStream

/**
 * KScan's own Android entry point over the corpus.
 *
 * The comparison test measures the decoders directly; this one goes through
 * `scanImage`, so the format mapping, the payload bytes and the result plumbing
 * are exercised rather than assumed.
 */
class ScanImageTest {
    @Test
    fun `GIVEN a clean frame THEN every format scans to its payload`() {
        val failures =
            BarcodeCorpus.SAMPLES
                .filter { it.condition.name == "baseline" }
                .mapNotNull { sample ->
                    val barcode = scan(sample)

                    when {
                        barcode == null -> "${sample.id} was not found"
                        !sample.matches(barcode.data) -> "${sample.id} read as ${barcode.data}"
                        barcode.rawBytes.isEmpty() -> "${sample.id} came back with no raw bytes"
                        else -> null
                    }
                }

        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun `GIVEN a format that is not asked for THEN nothing is returned`() {
        val sample = BarcodeCorpus.SAMPLES.first { it.format.name == "QR_CODE" && it.condition.name == "baseline" }

        val barcode = scan(sample, codeTypes = listOf(org.ncgroup.kscan.BarcodeFormat.FORMAT_EAN_13))

        assertEquals(null, barcode)
    }

    @Test
    fun `GIVEN a filter that rejects everything THEN nothing is returned`() {
        val sample = BarcodeCorpus.SAMPLES.first { it.format.name == "QR_CODE" && it.condition.name == "baseline" }

        val barcode = scan(sample, filter = { false })

        assertEquals(null, barcode)
    }

    @Test
    fun `GIVEN bytes that are not an image THEN it fails rather than throwing`() {
        var failure: Exception? = null

        scanImage(imageBytes = byteArrayOf(1, 2, 3)) { result ->
            failure = (result as? BarcodeResult.OnFailed)?.exception
        }

        assertEquals(true, failure != null)
    }

    private fun scan(
        sample: Sample,
        codeTypes: List<org.ncgroup.kscan.BarcodeFormat> = listOf(org.ncgroup.kscan.BarcodeFormat.FORMAT_ALL_FORMATS),
        filter: (Barcode) -> Boolean = { true },
    ): Barcode? {
        var scanned: Barcode? = null

        scanImage(imageBytes = sample.png(), codeTypes = codeTypes, filter = filter) { result ->
            scanned = (result as? BarcodeResult.OnSuccess)?.barcode
        }

        return scanned
    }

    private fun Sample.png(): ByteArray {
        val bitmap = render().toBitmap()

        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            bitmap.recycle()
            stream.toByteArray()
        }
    }
}
