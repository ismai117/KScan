package org.ncgroup.kscan

import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UNKNOWN
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarcodeFormatMapperTest {

    @Test
    fun `GIVEN empty list WHEN toMlKitFormats THEN returns all formats`() {
        val result = BarcodeFormatMapper.toMlKitFormats(emptyList())

        assertEquals(FORMAT_ALL_FORMATS, result)
    }

    @Test
    fun `GIVEN all formats WHEN toMlKitFormats THEN returns all formats`() {
        val result = BarcodeFormatMapper.toMlKitFormats(listOf(BarcodeFormat.FORMAT_ALL_FORMATS))

        assertEquals(FORMAT_ALL_FORMATS, result)
    }

    @Test
    fun `GIVEN single format WHEN toMlKitFormats THEN returns mlkit format`() {
        val result = BarcodeFormatMapper.toMlKitFormats(listOf(BarcodeFormat.FORMAT_QR_CODE))

        assertEquals(FORMAT_QR_CODE, result)
    }

    @Test
    fun `GIVEN multiple formats WHEN toMlKitFormats THEN returns combined flags`() {
        val result = BarcodeFormatMapper.toMlKitFormats(
            listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.FORMAT_EAN_13)
        )

        assertEquals(FORMAT_QR_CODE or FORMAT_EAN_13, result)
    }

    @Test
    fun `GIVEN qr code mlkit format WHEN toAppFormat THEN returns app format`() {
        val result = BarcodeFormatMapper.toAppFormat(FORMAT_QR_CODE)

        assertEquals(BarcodeFormat.FORMAT_QR_CODE, result)
    }

    @Test
    fun `GIVEN ean13 mlkit format WHEN toAppFormat THEN returns app format`() {
        val result = BarcodeFormatMapper.toAppFormat(FORMAT_EAN_13)

        assertEquals(BarcodeFormat.FORMAT_EAN_13, result)
    }

    @Test
    fun `GIVEN code128 mlkit format WHEN toAppFormat THEN returns app format`() {
        val result = BarcodeFormatMapper.toAppFormat(FORMAT_CODE_128)

        assertEquals(BarcodeFormat.FORMAT_CODE_128, result)
    }

    @Test
    fun `GIVEN unknown mlkit format WHEN toAppFormat THEN returns type unknown`() {
        val result = BarcodeFormatMapper.toAppFormat(FORMAT_UNKNOWN)

        assertEquals(BarcodeFormat.TYPE_UNKNOWN, result)
    }

    @Test
    fun `GIVEN unmapped mlkit format WHEN toAppFormat THEN returns type unknown`() {
        val result = BarcodeFormatMapper.toAppFormat(999999)

        assertEquals(BarcodeFormat.TYPE_UNKNOWN, result)
    }

    @Test
    fun `GIVEN known mlkit format WHEN isKnownFormat THEN returns true`() {
        val result = BarcodeFormatMapper.isKnownFormat(FORMAT_QR_CODE)

        assertTrue(result)
    }

    @Test
    fun `GIVEN unknown mlkit format WHEN isKnownFormat THEN returns true`() {
        val result = BarcodeFormatMapper.isKnownFormat(FORMAT_UNKNOWN)

        assertTrue(result)
    }

    @Test
    fun `GIVEN unmapped mlkit format WHEN isKnownFormat THEN returns false`() {
        val result = BarcodeFormatMapper.isKnownFormat(999999)

        assertFalse(result)
    }
}
