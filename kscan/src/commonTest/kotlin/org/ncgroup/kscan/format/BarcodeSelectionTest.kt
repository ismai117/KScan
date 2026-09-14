package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarcodeSelectionTest {
    @Test
    fun `GIVEN explicit formats THEN only those are requested`() {
        val codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE)

        assertTrue(isRequestedFormat(BarcodeFormat.FORMAT_QR_CODE, codeTypes))
        assertFalse(isRequestedFormat(BarcodeFormat.FORMAT_EAN_13, codeTypes))
    }

    @Test
    fun `GIVEN all formats THEN any recognised format is requested`() {
        val codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS)

        assertTrue(isRequestedFormat(BarcodeFormat.FORMAT_EAN_13, codeTypes))
        assertFalse(isRequestedFormat(BarcodeFormat.TYPE_UNKNOWN, codeTypes))
    }

    @Test
    fun `GIVEN no formats THEN behaves as all formats`() {
        assertTrue(isRequestedFormat(BarcodeFormat.FORMAT_EAN_13, emptyList()))
        assertFalse(isRequestedFormat(BarcodeFormat.TYPE_UNKNOWN, emptyList()))
    }

    @Test
    fun `GIVEN the unknown type is asked for THEN nothing satisfies it`() {
        // It maps to no platform format, which a decoder reads as no restriction,
        // so anything it found would otherwise come back reported as unknown.
        val codeTypes = listOf(BarcodeFormat.TYPE_UNKNOWN)

        assertFalse(isRequestedFormat(BarcodeFormat.TYPE_UNKNOWN, codeTypes))
        assertFalse(isRequestedFormat(BarcodeFormat.FORMAT_QR_CODE, codeTypes))
        assertFalse(isRequestedFormat(BarcodeFormat.FORMAT_EAN_13, codeTypes))
    }

    @Test
    fun `GIVEN named formats THEN an unnamed symbology is still rejected`() {
        val codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.TYPE_UNKNOWN)

        assertTrue(isRequestedFormat(BarcodeFormat.FORMAT_QR_CODE, codeTypes))
        assertFalse(isRequestedFormat(BarcodeFormat.TYPE_UNKNOWN, codeTypes))
    }
}
