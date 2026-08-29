package org.ncgroup.kscan

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Internal wrapper that handles the common scanner view layout.
 * Renders the camera content and optionally overlays the default scanner UI.
 */
@Composable
internal fun ScannerViewContent(
    colors: ScannerColors,
    scannerUiOptions: ScannerUiOptions?,
    torchEnabled: Boolean,
    onTorchChange: (Boolean) -> Unit,
    zoomRatio: Float,
    onZoomChange: (Float) -> Unit,
    maxZoomRatio: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cameraContent: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        cameraContent()

        if (scannerUiOptions != null) {
            ScannerUI(
                onCancel = onCancel,
                torchEnabled = torchEnabled,
                onTorchChange = onTorchChange,
                zoomRatio = zoomRatio,
                zoomRatioOnChange = onZoomChange,
                maxZoomRatio = maxZoomRatio,
                colors = colors,
                options = scannerUiOptions,
            )
        }
    }
}
