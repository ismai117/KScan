package org.ncgroup.kscan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Controller for managing scanner functionalities like torch and zoom.
 *
 * This class provides a way to control the scanner's torch (flash) and zoom level.
 * It uses mutable state properties that can be observed for changes, allowing UI
 * updates in response to scanner state modifications.
 *
 * Pass an instance to [ScannerView] and drive it from your own controls. It is
 * live only while that scanner is composed: [setTorch] and [setZoom] do nothing
 * before or after, and desktop cameras expose neither, so [maxZoomRatio] stays at
 * `1f` there and a zoom control has nothing to span.
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
public class ScannerController {
    public var torchEnabled: Boolean by mutableStateOf(false)
        internal set

    public var zoomRatio: Float by mutableFloatStateOf(1f)
        internal set

    public var maxZoomRatio: Float by mutableFloatStateOf(1f)
        internal set

    internal var onTorchChange: ((Boolean) -> Unit)? = null
    internal var onZoomChange: ((Float) -> Unit)? = null

    public fun setTorch(enabled: Boolean) {
        onTorchChange?.invoke(enabled)
    }

    public fun setZoom(ratio: Float) {
        onZoomChange?.invoke(ratio)
    }
}
