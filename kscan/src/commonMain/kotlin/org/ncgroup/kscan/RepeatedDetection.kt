package org.ncgroup.kscan

/**
 * Requires the same barcode to be decoded more than once before it is reported.
 *
 * A single frame can be decoded confidently and wrongly when the image is blurred
 * or partly obscured, and asking for the same value twice costs one frame while
 * making that far less likely.
 */
internal class RepeatedDetection(private val required: Int = 2) {
    private val counts = mutableMapOf<String, Int>()

    /** Records a sighting of [key] and returns whether it has now been seen enough. */
    fun accept(key: String): Boolean {
        val seen = (counts[key] ?: 0) + 1
        counts[key] = seen

        return seen >= required
    }

    fun reset() {
        counts.clear()
    }
}

/** Zoom beyond this is rarely usable for scanning. */
internal const val MAX_ZOOM_RATIO = 5.0f
