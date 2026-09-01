package org.ncgroup.kscan

/** The outcome of a scan. */
public sealed interface BarcodeResult {
    public data class OnSuccess(val barcode: Barcode) : BarcodeResult

    public data class OnFailed(val exception: Exception) : BarcodeResult
}
