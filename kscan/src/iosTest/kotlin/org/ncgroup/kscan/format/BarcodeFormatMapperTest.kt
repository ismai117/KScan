package org.ncgroup.kscan.format

import org.ncgroup.kscan.BarcodeFormat
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeInterleaved2of5Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BarcodeFormatMapperTest {

    @Test
    fun `GIVEN empty list WHEN toAvTypes THEN returns the same as all formats`() {
        assertEquals(
            BarcodeFormatMapper.toAvTypes(listOf(BarcodeFormat.FORMAT_ALL_FORMATS)),
            BarcodeFormatMapper.toAvTypes(emptyList()),
        )
    }

    @Test
    fun `GIVEN all formats WHEN toAvTypes THEN returns every mapped type`() {
        val result = BarcodeFormatMapper.toAvTypes(listOf(BarcodeFormat.FORMAT_ALL_FORMATS))

        assertTrue(AVMetadataObjectTypeQRCode in result)
        assertTrue(AVMetadataObjectTypeInterleaved2of5Code in result)
    }

    @Test
    fun `GIVEN single format WHEN toAvTypes THEN returns av type`() {
        val result = BarcodeFormatMapper.toAvTypes(listOf(BarcodeFormat.FORMAT_QR_CODE))

        assertEquals(listOf(AVMetadataObjectTypeQRCode), result)
    }

    @Test
    fun `GIVEN multiple formats WHEN toAvTypes THEN returns av types`() {
        val result = BarcodeFormatMapper.toAvTypes(
            listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.FORMAT_EAN_13),
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
    fun `GIVEN interleaved2of5 av type WHEN toAppFormat THEN returns app format`() {
        val result = BarcodeFormatMapper.toAppFormat(AVMetadataObjectTypeInterleaved2of5Code)

        assertEquals(BarcodeFormat.FORMAT_ITF, result)
    }

    @Test
    fun `GIVEN unknown av type WHEN toAppFormat THEN returns type unknown`() {
        val result = BarcodeFormatMapper.toAppFormat("unknown_type")

        assertEquals(BarcodeFormat.TYPE_UNKNOWN, result)
    }
}
