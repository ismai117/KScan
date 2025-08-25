package org.ncgroup.kscan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A composable function that displays a scanner view for scanning barcodes.
 *
 * @param modifier The modifier to be applied to the scanner view.
 * @param codeTypes The list of barcode formats to be scanned.
 * @param colors The colors to be used for the scanner view.
 * @param showUi A boolean indicating whether to show the UI elements of the scanner view.
 * @param scannerController An optional controller for controlling the scanner.
 * @param filter An optional lambda which can be used to filter out results before receiving a [BarcodeResult]
 * @param result A callback function that is invoked when a barcode is scanned.
 */
@Composable
expect fun ScannerView(
    modifier: Modifier = Modifier.fillMaxSize(),
    codeTypes: List<BarcodeFormat>,
    colors: ScannerColors = scannerColors(),
    showUi: Boolean = true,
    scannerController: ScannerController? = null,
    filter: (Barcode) -> Boolean = { true },
    result: (BarcodeResult) -> Unit,
)
