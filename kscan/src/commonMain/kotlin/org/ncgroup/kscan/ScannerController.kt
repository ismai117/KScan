package org.ncgroup.kscan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Controller for managing scanner functionalities like torch and zoom.
 *
 * This class provides a way to control the scanner's torch (flash) and zoom level.
 * It uses mutable state properties that can be observed for changes, allowing UI
 * updates in response to scanner state modifications.
 *
 * @property torchEnabled A boolean indicating whether the torch is currently enabled.
 *                        Defaults to `false`. This property is read-only and can be observed for changes.
 *                        Use [setTorch] to toggle the torch.
 * @property zoomRatio The current zoom ratio of the scanner. Defaults to `1f`.
 *                     This property is read-only and can be observed for changes.
 *                     The valid range is typically between 1f and [maxZoomRatio].
 *                     Use [setZoom] to change the zoom level.
 * @property maxZoomRatio The maximum zoom ratio supported by the scanner. Defaults to `1f`.
 *                        This property is read-only and is set internally.
 */
class ScannerController {
    var torchEnabled by mutableStateOf(false)
        internal set

    var zoomRatio by mutableStateOf(1f)
        internal set

    var maxZoomRatio by mutableStateOf(1f)
        internal set

    internal var onTorchChange: ((Boolean) -> Unit)? = null
    internal var onZoomChange: ((Float) -> Unit)? = null

    fun setTorch(enabled: Boolean) {
        onTorchChange?.invoke(enabled)
    }

    fun setZoom(ratio: Float) {
        onZoomChange?.invoke(ratio)
    }
}
