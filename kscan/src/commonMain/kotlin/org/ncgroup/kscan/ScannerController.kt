package org.ncgroup.kscan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Drives the torch and zoom of the [ScannerView] it is passed to.
 *
 * The properties are observable state. [setTorch] and [setZoom] do nothing while
 * that scanner is not composed, and desktop supports neither.
 *
 * @property torchEnabled Whether the torch is on.
 * @property zoomRatio The current zoom, between `1f` and [maxZoomRatio].
 * @property maxZoomRatio The highest zoom the camera offers, or `1f` when it offers none.
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
