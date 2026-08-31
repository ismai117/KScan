package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.google.zxing.BarcodeFormat as ZxingBarcodeFormat

class BarcodeFormatMapperTest {

    @Test
    fun `GIVEN ZxingBarcodeFormat WHEN toKScanFormat THEN returns correct BarcodeFormat`() {
        assertEquals(BarcodeFormat.FORMAT_QR_CODE, ZxingBarcodeFormat.QR_CODE.toKScanFormat())
        assertEquals(BarcodeFormat.FORMAT_EAN_13, ZxingBarcodeFormat.EAN_13.toKScanFormat())
        assertEquals(BarcodeFormat.FORMAT_CODE_128, ZxingBarcodeFormat.CODE_128.toKScanFormat())
        assertEquals(BarcodeFormat.FORMAT_AZTEC, ZxingBarcodeFormat.AZTEC.toKScanFormat())
        assertEquals(BarcodeFormat.FORMAT_DATA_MATRIX, ZxingBarcodeFormat.DATA_MATRIX.toKScanFormat())

        assertEquals(BarcodeFormat.TYPE_UNKNOWN, ZxingBarcodeFormat.MAXICODE.toKScanFormat()) // Assuming MAXICODE is unsupported
    }

    @Test
    fun `GIVEN BarcodeFormat WHEN toZxingFormat THEN returns correct ZxingBarcodeFormat`() {
        assertEquals(ZxingBarcodeFormat.QR_CODE, BarcodeFormat.FORMAT_QR_CODE.toZxingFormat())
        assertEquals(ZxingBarcodeFormat.EAN_13, BarcodeFormat.FORMAT_EAN_13.toZxingFormat())
        assertEquals(ZxingBarcodeFormat.CODE_128, BarcodeFormat.FORMAT_CODE_128.toZxingFormat())
        assertEquals(ZxingBarcodeFormat.AZTEC, BarcodeFormat.FORMAT_AZTEC.toZxingFormat())
        assertEquals(ZxingBarcodeFormat.DATA_MATRIX, BarcodeFormat.FORMAT_DATA_MATRIX.toZxingFormat())

        assertNull(BarcodeFormat.TYPE_UNKNOWN.toZxingFormat())
    }

    @Test
    fun `GIVEN BarcodeFormat WHEN round-trip to Zxing and back THEN returns original`() {
        val original = BarcodeFormat.FORMAT_QR_CODE
        val zxing = original.toZxingFormat()
        val back = zxing?.toKScanFormat()
        assertEquals(original, back)

        // Test another
        val original2 = BarcodeFormat.FORMAT_EAN_13
        val zxing2 = original2.toZxingFormat()
        val back2 = zxing2?.toKScanFormat()
        assertEquals(original2, back2)

        // Unsupported round-trip should go to TYPE_UNKNOWN
        val unsupported = BarcodeFormat.TYPE_UNKNOWN
        assertNull(unsupported.toZxingFormat()) // No ZXing equivalent
    }

    @Test
    fun `GIVEN codeTypes list WHEN checking hasAllFormats THEN behaves correctly`() {
        // With ALL_FORMATS
        val allFormatsList = listOf(BarcodeFormat.FORMAT_ALL_FORMATS)
        assertTrue(allFormatsList.contains(BarcodeFormat.FORMAT_ALL_FORMATS))
        val allMapped = allFormatsList.mapNotNull { it.toZxingFormat() }
        assertTrue(allMapped.isEmpty()) // ALL_FORMATS has no direct ZXing, so no hint -> all allowed
    }

    @Test
    fun `GIVEN unmapped ZxingBarcodeFormat WHEN toKScanFormat THEN returns TYPE_UNKNOWN`() {
        assertEquals(BarcodeFormat.TYPE_UNKNOWN, ZxingBarcodeFormat.MAXICODE.toKScanFormat())
    }

    @Test
    fun `GIVEN list with unsupported formats WHEN toZxingFormat THEN filters them out`() {
        val input = listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.TYPE_UNKNOWN)
        val mapped = input.mapNotNull { it.toZxingFormat() }
        assertEquals(listOf(ZxingBarcodeFormat.QR_CODE), mapped)
    }
}
