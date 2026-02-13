package org.ncgroup.kscan

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
    scannerUiOptions: ScannerUiOptions?,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit,
) {
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
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

    val onTorchChange =
        remember {
            { enabled: Boolean ->
                if (captureDevice.hasTorch) {
                    val prev = torchEnabled
                    var locked = false
                    try {
                        locked = captureDevice.lockForConfiguration(null)
                        if (locked) {
                            captureDevice.torchMode =
                                if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
                            torchEnabled = enabled
                            scannerController?.torchEnabled = enabled
                        }
                    } catch (e: Throwable) {
                        // Revert state and report
                        torchEnabled = prev
                        scannerController?.torchEnabled = prev
                        result(
                            BarcodeResult.OnFailed(
                                RuntimeException(
                                    e.message ?: "Torch toggle failed", e
                                )
                            )
                        )
                    } finally {
                        if (locked) {
                            captureDevice.unlockForConfiguration()
                        }
                    }
                }
            }
        }

    scannerController?.onTorchChange = onTorchChange

    val cameraViewController = remember {
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

    scannerController?.onZoomChange = { ratio ->
        cameraViewController.setZoom(ratio)
        zoomRatio = ratio
        scannerController.zoomRatio = ratio
    }

    scannerController?.maxZoomRatio = maxZoomRatio

    ScannerViewContent(
        modifier = modifier,
        colors = colors,
        scannerUiOptions = scannerUiOptions,
        torchEnabled = torchEnabled,
        onTorchEnabled = onTorchChange,
        zoomRatio = zoomRatio,
        onZoomChange = { ratio ->
            cameraViewController.setZoom(ratio)
            zoomRatio = ratio
        },
        maxZoomRatio = maxZoomRatio,
        onCancel = {
            result(BarcodeResult.OnCanceled)
            cameraViewController.dispose()
        },
    ) {
        UIKitViewController(
            factory = { cameraViewController },
            modifier = Modifier.fillMaxSize(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraViewController.dispose()
        }
    }
}
