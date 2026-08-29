package org.ncgroup.kscan

/**
 * Represents a scanned barcode.
 *
 * @property data The decoded String data from the barcode.
 * @property format The format of the barcode (e.g., QR_CODE, EAN_13).
 * @property rawBytes The raw byte data from the barcode.
 */
public data class Barcode(
    val data: String,
    val format: String,
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
