package org.ncgroup.kscan.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.scanner.CameraViewController
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.torchMode

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun ScannerViewImpl(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") cameraId: String?,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    autoZoom: Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val updatedResult by rememberUpdatedState(result)
    val updatedFilter by rememberUpdatedState(filter)

    val captureDevice =
        remember {
            AVCaptureDevice.defaultDeviceWithDeviceType(
                AVCaptureDeviceTypeBuiltInWideAngleCamera,
                AVMediaTypeVideo,
                AVCaptureDevicePositionBack,
            )
        }

    if (captureDevice == null) {
        DisposableEffect(Unit) {
            updatedResult(
                BarcodeResult.OnFailed(
                    Exception("No back camera available"),
                ),
            )

            onDispose {}
        }

        return
    }

    val cameraViewController =
        remember(captureDevice, codeTypes) {
            CameraViewController(
                device = captureDevice,
                codeTypes = codeTypes,
                filter = { barcode -> updatedFilter(barcode) },
                onBarcodeSuccess = { scannedBarcodes ->
                    updatedResult(
                        BarcodeResult.OnSuccess(
                            scannedBarcodes.first(),
                        ),
                    )
                },
                onBarcodeFailed = { error ->
                    updatedResult(
                        BarcodeResult.OnFailed(error),
                    )
                },
                onMaxZoomRatioAvailable = { maxRatio ->
                    scannerController?.maxZoomRatio = maxRatio
                },
            )
        }

    DisposableEffect(
        scannerController,
        captureDevice,
        cameraViewController,
    ) {
        val onTorchChange: (Boolean) -> Unit = { enabled ->
            var locked = false

            try {
                locked = captureDevice.lockForConfiguration(null)

                if (locked) {
                    captureDevice.torchMode =
                        if (enabled) {
                            AVCaptureTorchModeOn
                        } else {
                            AVCaptureTorchModeOff
                        }

                    scannerController?.torchEnabled = enabled
                }
            } catch (e: Throwable) {
                updatedResult(
                    BarcodeResult.OnFailed(
                        RuntimeException(
                            e.message ?: "Torch toggle failed",
                            e,
                        ),
                    ),
                )
            } finally {
                if (locked) {
                    captureDevice.unlockForConfiguration()
                }
            }
        }

        val onZoomChange: (Float) -> Unit = { ratio ->
            scannerController?.zoomRatio = cameraViewController.setZoom(ratio)
        }

        scannerController?.onTorchChange = onTorchChange
        scannerController?.onZoomChange = onZoomChange

        onDispose {
            scannerController?.onTorchChange = null
            scannerController?.onZoomChange = null
            cameraViewController.dispose()
        }
    }

    UIKitViewController(
        factory = { cameraViewController },
        modifier = modifier,
    )
}
