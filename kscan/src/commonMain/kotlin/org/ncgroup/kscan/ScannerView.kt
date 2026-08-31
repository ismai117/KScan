package org.ncgroup.kscan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Draws the camera preview and reports the barcodes decoded from it.
 *
 * The preview is all this draws: a torch button, a zoom control, a close
 * affordance and any overlay are the caller's to build. Pass a
 * [ScannerController] to drive torch and zoom from those controls.
 *
 * @param codeTypes The barcode formats to scan for, or [BarcodeFormat.FORMAT_ALL_FORMATS] for every supported format.
 * @param modifier The modifier applied to the preview. Fills the available space by default.
 * @param scannerController An optional controller for torch and zoom.
 * @param filter Called with each decoded barcode; returning `false` keeps scanning.
 * @param autoZoom Lets the decoder zoom the camera in on a barcode too small to
 *   read. Only Android has a decoder that asks for this; elsewhere it is ignored.
 * @param result A callback function that is invoked when a barcode is scanned.
 */
@Composable
public expect fun ScannerView(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier = Modifier.fillMaxSize(),
    scannerController: ScannerController? = null,
    filter: (Barcode) -> Boolean = { true },
    autoZoom: Boolean = true,
    result: (BarcodeResult) -> Unit,
)
