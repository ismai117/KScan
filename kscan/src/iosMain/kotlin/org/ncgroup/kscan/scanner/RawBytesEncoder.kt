package org.ncgroup.kscan.scanner

internal fun stringToRawBytes(value: String): ByteArray = ByteArray(value.length) { value[it].code.toByte() }
