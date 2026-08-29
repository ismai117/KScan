package org.ncgroup.kscan

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
}
