package org.ncgroup.kscan

/**
 * A decoded barcode.
 *
 * @property data The decoded text.
 * @property format The format it was decoded as.
 * @property rawBytes The barcode's own bytes, or [data] encoded where the
 *   decoder does not expose them.
 */
public data class Barcode(
    val data: String,
    val format: BarcodeFormat,
    val rawBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Barcode

        if (data != other.data) return false
        if (format != other.format) return false
        if (!rawBytes.contentEquals(other.rawBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + rawBytes.contentHashCode()
        return result
    }
}
