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
 * @param scannerUiOptions The UI options to be used for the scanner UI. Will hide the UI if set to `null`.
 * @param scannerController An optional controller for controlling the scanner.
 * @param filter An optional lambda which can be used to filter out results before receiving a [BarcodeResult]
 * @param result A callback function that is invoked when a barcode is scanned.
 */
@Composable
public expect fun ScannerView(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier = Modifier.fillMaxSize(),
    colors: ScannerColors = ScannerColors(),
    scannerUiOptions: ScannerUiOptions? = ScannerUiOptions(),
    scannerController: ScannerController? = null,
    filter: (Barcode) -> Boolean = { true },
    result: (BarcodeResult) -> Unit,
)
