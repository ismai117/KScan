package org.ncgroup.kscan

/**
 * Represents the result of a barcode scanning operation.
 * It can be one of three states:
 * - [OnSuccess]: Indicates that a barcode was successfully scanned.
 * - [OnFailed]: Indicates that the barcode scanning failed due to an error.
 * - [OnCanceled]: Indicates that a decode was canceled before it finished.
 */
public sealed interface BarcodeResult {
    public data class OnSuccess(val barcode: Barcode) : BarcodeResult

    public data class OnFailed(val exception: Exception) : BarcodeResult

    public data object OnCanceled : BarcodeResult
}
