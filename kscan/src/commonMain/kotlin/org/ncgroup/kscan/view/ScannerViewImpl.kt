package org.ncgroup.kscan.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerController

// Every parameter is passed explicitly: the defaults and the published signature
// belong to ScannerView, which is the only caller.
@Suppress("ktlint:compose:modifier-without-default-check")
@Composable
internal expect fun ScannerViewImpl(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier,
    cameraId: String?,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    autoZoom: Boolean,
    result: (BarcodeResult) -> Unit,
)
