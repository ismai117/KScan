package org.ncgroup.kscan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    colors: ScannerColors,
    showUi: Boolean,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }

    var cameraViewController by remember { mutableStateOf<CameraViewController?>(null) }
    val captureDevice: AVCaptureDevice? =
        remember {
            AVCaptureDevice.defaultDeviceWithDeviceType(
                AVCaptureDeviceTypeBuiltInWideAngleCamera,
                AVMediaTypeVideo,
                AVCaptureDevicePositionBack,
            )
        }

    if (captureDevice == null) {
        result(BarcodeResult.OnFailed(Exception("No back camera available")))
        return
    }

    scannerController?.onTorchChange = { enabled ->
        runCatching {
            if (captureDevice.hasTorch) {
                captureDevice.lockForConfiguration(null)
                captureDevice.torchMode =
                    if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                captureDevice.unlockForConfiguration()
                torchEnabled = enabled
                scannerController.torchEnabled = enabled
            }
        }
    }

    scannerController?.onZoomChange = { ratio ->
        cameraViewController?.setZoom(ratio)
        zoomRatio = ratio
        scannerController.zoomRatio = ratio
    }

    scannerController?.maxZoomRatio = maxZoomRatio

    cameraViewController =
        remember {
            CameraViewController(
                device = captureDevice,
                codeTypes = codeTypes,
                filter = filter,
                onBarcodeSuccess = { scannedBarcodes ->
                    result(BarcodeResult.OnSuccess(scannedBarcodes.first()))
                },
                onBarcodeFailed = { error ->
                    result(BarcodeResult.OnFailed(error))
                },
                onBarcodeCanceled = {
                    result(BarcodeResult.OnCanceled)
                },
                onMaxZoomRatioAvailable = { maxRatio ->
                    maxZoomRatio = maxRatio
                }
            )
        }

    Box(modifier = modifier) {
        UIKitViewController(
            factory = { cameraViewController!! },
            modifier = Modifier.fillMaxSize(),
        )

        if (showUi) {
            ScannerUI(
                onCancel = {
                    result(BarcodeResult.OnCanceled)
                    cameraViewController = null
                },
                torchEnabled = torchEnabled,
                onTorchEnabled = { enabled ->
                    runCatching {
                        if (captureDevice.hasTorch) {
                            captureDevice.lockForConfiguration(null)
                            captureDevice.torchMode =
                                if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                            captureDevice.unlockForConfiguration()
                            torchEnabled = enabled
                        }
                    }
                },
                zoomRatio = zoomRatio,
                zoomRatioOnChange = { ratio ->
                    cameraViewController?.setZoom(ratio)
                    zoomRatio = ratio
                },
                maxZoomRatio = maxZoomRatio,
                colors = colors,
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraViewController = null
        }
    }
}
