@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ncgroup.kscan

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.ncgroup.kscan.format.BarcodeFormatMapper
import org.ncgroup.kscan.format.firstMatching
import org.ncgroup.kscan.scanner.barcodeDetector
import org.ncgroup.kscan.scanner.closeImageBitmap
import org.ncgroup.kscan.scanner.detectFrom
import org.ncgroup.kscan.scanner.imageBitmapFromBase64
import org.ncgroup.kscan.scanner.toBarcodes
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
public actual fun scanImage(
    imageBytes: ByteArray,
    codeTypes: List<BarcodeFormat>,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val scope = MainScope()

    scope.launch {
        try {
            val detector = barcodeDetector(
                formats = BarcodeFormatMapper.toWebFormats(codeTypes),
                polyfillUrl = KScanWeb.barcodeDetectorPolyfillUrl.orEmpty(),
                zxingWasmUrl = KScanWeb.zxingWasmUrl.orEmpty(),
                debug = KScanWeb.debugLogging,
            )

            val bitmap = imageBitmapFromBase64(
                data = Base64.encode(imageBytes),
                mimeType = sniffImageMimeType(imageBytes),
            ).await()

            val detectedBarcodes = try {
                detectFrom(detector, bitmap).await().toBarcodes()
            } finally {
                closeImageBitmap(bitmap)
            }

            if (detectedBarcodes.isEmpty()) {
                result(BarcodeResult.OnFailed(Exception("No barcode found in image")))
                return@launch
            }

            val matchingBarcode = detectedBarcodes.firstMatching(codeTypes, filter)

            if (matchingBarcode != null) {
                result(BarcodeResult.OnSuccess(matchingBarcode))
            } else {
                result(BarcodeResult.OnFailed(Exception("No matching barcode found in image")))
            }
        } catch (e: Throwable) {
            result(BarcodeResult.OnFailed(Exception(e.message ?: e.toString())))
        } finally {
            scope.cancel()
        }
    }
}

private fun sniffImageMimeType(imageBytes: ByteArray): String {
    fun matches(vararg signature: Int): Boolean {
        if (imageBytes.size < signature.size) return false
        return signature.withIndex().all { (index, byte) -> imageBytes[index] == byte.toByte() }
    }

    return when {
        matches(0x89, 0x50, 0x4E, 0x47) -> "image/png"

        matches(0xFF, 0xD8, 0xFF) -> "image/jpeg"

        matches(0x47, 0x49, 0x46, 0x38) -> "image/gif"

        matches(0x42, 0x4D) -> "image/bmp"

        matches(0x52, 0x49, 0x46, 0x46) &&
            imageBytes.size >= 12 &&
            imageBytes.copyOfRange(8, 12).decodeToString() == "WEBP" -> "image/webp"

        // An ISO base media file: a photo straight off a phone is usually HEIC.
        imageBytes.size >= 12 &&
            imageBytes.copyOfRange(4, 8).decodeToString() == "ftyp" -> {
            when (imageBytes.copyOfRange(8, 12).decodeToString()) {
                "heic", "heix", "hevc", "heim", "heis" -> "image/heic"
                "mif1", "msf1" -> "image/heif"
                "avif" -> "image/avif"
                else -> ""
            }
        }

        else -> ""
    }
}
