package org.ncgroup.kscan.scanner

import androidx.camera.core.Camera
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.BarcodeFormatMapper

// getCamera is read on each suggestion rather than captured: the camera is bound
// after the options are built.
internal fun barcodeScannerOptions(
    codeTypes: List<BarcodeFormat>,
    autoZoom: Boolean,
    getCamera: () -> Camera?,
): BarcodeScannerOptions = BarcodeScannerOptions.Builder()
    .setBarcodeFormats(BarcodeFormatMapper.toMlKitFormats(codeTypes))
    .apply {
        if (autoZoom) {
            setZoomSuggestionOptions(
                ZoomSuggestionOptions.Builder { zoomRatio ->
                    val camera = getCamera()
                    val maxZoomRatio =
                        (camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1.0f)
                            .coerceAtMost(MAX_ZOOM_RATIO)
                    if (zoomRatio <= maxZoomRatio) {
                        camera?.cameraControl?.setZoomRatio(zoomRatio)
                        true
                    } else {
                        false
                    }
                }.setMaxSupportedZoomRatio(MAX_ZOOM_RATIO).build(),
            )
        }
    }
    .build()
