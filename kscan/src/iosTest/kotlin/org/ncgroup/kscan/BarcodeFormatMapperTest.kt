package org.ncgroup.kscan

import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarcodeFormatMapperTest {

    @Test
    fun `GIVEN empty list WHEN toAvTypes THEN returns all supported types`() {
        val result = BarcodeFormatMapper.toAvTypes(emptyList())

        assertEquals(BarcodeFormatMapper.allSupportedTypes, result)
    }

    @Test
    fun `GIVEN all formats WHEN toAvTypes THEN returns all supported types`() {
        val result = BarcodeFormatMapper.toAvTypes(listOf(BarcodeFormat.FORMAT_ALL_FORMATS))

        assertEquals(BarcodeFormatMapper.allSupportedTypes, result)
    }

    @Test
    fun `GIVEN single format WHEN toAvTypes THEN returns av type`() {
        val result = BarcodeFormatMapper.toAvTypes(listOf(BarcodeFormat.FORMAT_QR_CODE))

        assertEquals(listOf(AVMetadataObjectTypeQRCode), result)
    }

    @Test
    fun `GIVEN multiple formats WHEN toAvTypes THEN returns av types`() {
        val result = BarcodeFormatMapper.toAvTypes(
            listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.FORMAT_EAN_13)
        )

        assertEquals(listOf(AVMetadataObjectTypeQRCode, AVMetadataObjectTypeEAN13Code), result)
    }

    @Test
    fun `GIVEN qr code av type WHEN toAppFormat THEN returns app format`() {
        val result = BarcodeFormatMapper.toAppFormat(AVMetadataObjectTypeQRCode)

        assertEquals(BarcodeFormat.FORMAT_QR_CODE, result)
    }

    @Test
    fun `GIVEN ean13 av type WHEN toAppFormat THEN returns app format`() {
        val result = BarcodeFormatMapper.toAppFormat(AVMetadataObjectTypeEAN13Code)

        assertEquals(BarcodeFormat.FORMAT_EAN_13, result)
    }

    @Test
    fun `GIVEN code128 av type WHEN toAppFormat THEN returns app format`() {
        val result = BarcodeFormatMapper.toAppFormat(AVMetadataObjectTypeCode128Code)

        assertEquals(BarcodeFormat.FORMAT_CODE_128, result)
    }

    @Test
    fun `GIVEN unknown av type WHEN toAppFormat THEN returns type unknown`() {
        val result = BarcodeFormatMapper.toAppFormat("unknown_type")

        assertEquals(BarcodeFormat.TYPE_UNKNOWN, result)
    }

    @Test
    fun `GIVEN known av type WHEN isKnownFormat THEN returns true`() {
        val result = BarcodeFormatMapper.isKnownFormat(AVMetadataObjectTypeQRCode)

        assertTrue(result)
    }

    @Test
    fun `GIVEN unknown av type WHEN isKnownFormat THEN returns false`() {
        val result = BarcodeFormatMapper.isKnownFormat("unknown_type")

        assertFalse(result)
    }
}
