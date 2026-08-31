package org.ncgroup.kscan.scanner

/**
 * Converts a barcode string value to raw bytes without UTF-8 encoding.
 */
internal fun stringToRawBytes(value: String): ByteArray = ByteArray(value.length) { value[it].code.toByte() }
