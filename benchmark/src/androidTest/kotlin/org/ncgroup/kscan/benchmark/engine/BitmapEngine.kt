package org.ncgroup.kscan.benchmark.engine

import android.graphics.Bitmap
import org.ncgroup.kscan.benchmark.corpus.BarcodeCorpus
import org.ncgroup.kscan.benchmark.corpus.Frame

/**
 * One decoder under test.
 *
 * The bitmap is built by the caller and handed to every engine, so the cost of
 * turning a corpus frame into an image is not charged to any of them.
 */
interface BitmapEngine {
    val name: String

    fun decode(bitmap: Bitmap): Decoded?

    fun close() = Unit
}

data class Decoded(
    val format: String,
    val text: String,
)

fun Frame.toBitmap(): Bitmap = Bitmap.createBitmap(
    pixels,
    BarcodeCorpus.FRAME_WIDTH,
    BarcodeCorpus.FRAME_HEIGHT,
    Bitmap.Config.ARGB_8888,
)
