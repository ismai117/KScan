package org.ncgroup.kscan.scanner

// A blurred or half-covered frame can decode confidently and wrongly, and asking
// for the same value twice costs one frame while making that far less likely.
internal class RepeatedDetection(private val required: Int = 2) {
    private val counts = mutableMapOf<String, Int>()

    fun accept(key: String): Boolean {
        val seen = (counts[key] ?: 0) + 1
        counts[key] = seen

        return seen >= required
    }

    fun reset() {
        counts.clear()
    }
}
