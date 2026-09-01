package org.ncgroup.kscan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ncgroup.kscan.view.ScannerViewImpl

/**
 * Draws the camera preview and reports the barcodes decoded from it.
 *
 * The preview is all this draws; controls and overlays are the caller's to build.
 * On web the preview is an HTML element the browser stacks above the Compose
 * canvas, so put them beside it rather than over it.
 *
 * @param codeTypes The formats to scan for.
 * @param modifier Applied to the preview. Fills the available space by default.
 * @param cameraId The camera to open, from `availableCameras()`. `null` lets the
 *   platform choose, preferring a rear-facing camera. Web and desktop only.
 * @param scannerController Drives torch and zoom from your own controls.
 * @param filter Called with each decoded barcode; returning `false` keeps scanning.
 * @param autoZoom Lets the decoder zoom in on a barcode too small to read. Android only.
 * @param result Called with the outcome. Scanning stops at the first match.
 */
@Composable
public fun ScannerView(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier = Modifier.fillMaxSize(),
    cameraId: String? = null,
    scannerController: ScannerController? = null,
    filter: (Barcode) -> Boolean = { true },
    autoZoom: Boolean = true,
    result: (BarcodeResult) -> Unit,
) {
    ScannerViewImpl(
        codeTypes = codeTypes,
        modifier = modifier,
        cameraId = cameraId,
        scannerController = scannerController,
        filter = filter,
        autoZoom = autoZoom,
        result = result,
    )
}
