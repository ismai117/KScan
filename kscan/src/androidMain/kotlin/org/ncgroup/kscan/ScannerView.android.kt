package org.ncgroup.kscan

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Composable function that displays a camera preview for scanning barcodes.
 *
 * @param modifier The modifier to be applied to the scanner view.
 * @param codeTypes A list of barcode formats to be scanned.
 * @param colors The colors to be used for the scanner UI.
 * @param showUi A boolean indicating whether to show the scanner UI.
 * @param scannerController An optional controller for managing scanner actions.
 * @param result A callback function that receives the barcode scanning result.
 */
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var initializationError: Throwable? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                try {
                    cameraProvider = future.get()
                } catch (e: Exception) {
                    initializationError = e
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }

    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }

    LaunchedEffect(camera) {
        camera?.cameraInfo?.torchState?.observe(lifecycleOwner) {
            torchEnabled = it == TorchState.ON
        }
    }
    LaunchedEffect(camera) {
        camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) {
            zoomRatio = it.zoomRatio
            maxZoomRatio = it.maxZoomRatio
        }
    }

    scannerController?.onTorchChange = { enabled ->
        cameraControl?.enableTorch(enabled)
        scannerController.torchEnabled = enabled
    }

    scannerController?.onZoomChange = { ratio ->
        cameraControl?.setZoomRatio(ratio)
        scannerController.zoomRatio = ratio
    }

    scannerController?.maxZoomRatio = maxZoomRatio

    Box(modifier = modifier) {
        when {
            initializationError != null -> {
                LaunchedEffect(initializationError) {
                    result(BarcodeResult.OnFailed(Exception(initializationError)))
                }
            }

            cameraProvider != null -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val preview = Preview.Builder().build()
                        val selector =
                            CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()

                        preview.surfaceProvider = previewView.surfaceProvider

                        val imageAnalysis =
                            ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                        imageAnalysis.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx),
                            BarcodeAnalyzer(
                                camera = camera,
                                codeTypes = codeTypes,
                                onSuccess = { scannedBarcodes ->
                                    result(BarcodeResult.OnSuccess(scannedBarcodes.first()))
                                    cameraProvider?.unbind(imageAnalysis)
                                },
                                onFailed = { result(BarcodeResult.OnFailed(Exception(it))) },
                                onCanceled = { result(BarcodeResult.OnCanceled) },
                                filter = filter
                            ),
                        )

                        camera =
                            bindCamera(
                                lifecycleOwner = lifecycleOwner,
                                cameraProviderFuture = cameraProvider,
                                selector = selector,
                                preview = preview,
                                imageAnalysis = imageAnalysis,
                                result = result,
                                cameraControl = { cameraControl = it },
                            )

                        previewView
                    },
                    onRelease = {
                        cameraProvider?.unbindAll()
                    },
                )
            }
            else -> {}
        }

        if (showUi) {
            ScannerUI(
                onCancel = { result(BarcodeResult.OnCanceled) },
                torchEnabled = torchEnabled,
                onTorchEnabled = { cameraControl?.enableTorch(it) },
                zoomRatio = zoomRatio,
                zoomRatioOnChange = { ratio -> cameraControl?.setZoomRatio(ratio) },
                maxZoomRatio = maxZoomRatio,
                colors = colors,
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            camera = null
            cameraControl = null
        }
    }
}

internal fun bindCamera(
    lifecycleOwner: LifecycleOwner,
    cameraProviderFuture: ProcessCameraProvider?,
    selector: CameraSelector,
    preview: Preview,
    imageAnalysis: ImageAnalysis,
    result: (BarcodeResult) -> Unit,
    cameraControl: (CameraControl?) -> Unit,
): Camera? {
    return runCatching {
        cameraProviderFuture?.unbindAll()
        cameraProviderFuture?.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            imageAnalysis,
        ).also {
            cameraControl(it?.cameraControl)
        }
    }.getOrElse {
        result(BarcodeResult.OnFailed(Exception(it)))
        null
    }
}
