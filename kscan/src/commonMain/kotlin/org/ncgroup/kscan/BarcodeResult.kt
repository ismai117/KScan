package org.ncgroup.kscan

/**
 * Represents the result of a barcode scanning operation.
 * It is one of two states:
 * - [OnSuccess]: a barcode was scanned.
 * - [OnFailed]: scanning failed.
 */
public sealed interface BarcodeResult {
    public data class OnSuccess(val barcode: Barcode) : BarcodeResult

    public data class OnFailed(val exception: Exception) : BarcodeResult
}
