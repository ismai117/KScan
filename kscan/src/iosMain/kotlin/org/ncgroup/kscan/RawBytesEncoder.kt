package org.ncgroup.kscan

/**
 * Converts a barcode string value to raw bytes without UTF-8 encoding.
 */
internal fun stringToRawBytes(value: String): ByteArray {
    return ByteArray(value.length) { value[it].code.toByte() }
}
