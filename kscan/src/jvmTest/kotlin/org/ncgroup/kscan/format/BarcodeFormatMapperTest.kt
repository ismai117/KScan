package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import com.google.zxing.BarcodeFormat as ZxingBarcodeFormat

class BarcodeFormatMapperTest {

    @Test
    fun `GIVEN empty list WHEN toZxingFormats THEN returns the same as all formats`() {
        assertEquals(
            BarcodeFormatMapper.toZxingFormats(listOf(BarcodeFormat.FORMAT_ALL_FORMATS)),
            BarcodeFormatMapper.toZxingFormats(emptyList()),
        )
    }

    @Test
    fun `GIVEN formats WHEN toZxingFormats THEN returns zxing formats`() {
        val result = BarcodeFormatMapper.toZxingFormats(
            listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.FORMAT_EAN_13),
        )

        assertEquals(listOf(ZxingBarcodeFormat.QR_CODE, ZxingBarcodeFormat.EAN_13), result)
    }

    @Test
    fun `GIVEN unmappable format WHEN toZxingFormats THEN drops it`() {
        val result = BarcodeFormatMapper.toZxingFormats(
            listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.TYPE_UNKNOWN),
        )

        assertEquals(listOf(ZxingBarcodeFormat.QR_CODE), result)
    }

    @Test
    fun `GIVEN zxing format WHEN toAppFormat THEN returns app format`() {
        assertEquals(
            BarcodeFormat.FORMAT_QR_CODE,
            BarcodeFormatMapper.toAppFormat(ZxingBarcodeFormat.QR_CODE),
        )
        assertEquals(
            BarcodeFormat.FORMAT_DATA_MATRIX,
            BarcodeFormatMapper.toAppFormat(ZxingBarcodeFormat.DATA_MATRIX),
        )
    }

    @Test
    fun `GIVEN unmapped zxing format WHEN toAppFormat THEN returns type unknown`() {
        assertEquals(
            BarcodeFormat.TYPE_UNKNOWN,
            BarcodeFormatMapper.toAppFormat(ZxingBarcodeFormat.MAXICODE),
        )
    }
}
