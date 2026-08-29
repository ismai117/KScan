package org.ncgroup.kscan

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
public actual fun ScannerView(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier,
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
    var analyzer: BarcodeAnalyzer? by remember { mutableStateOf(null) }

    val updatedResult by rememberUpdatedState(result)

    LaunchedEffect(camera) {
        camera?.cameraInfo?.torchState?.observe(lifecycleOwner) { state ->
            scannerController?.torchEnabled = state == TorchState.ON
        }
    }

    LaunchedEffect(camera) {
        camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
            scannerController?.zoomRatio = state.zoomRatio
            scannerController?.maxZoomRatio = state.maxZoomRatio
        }
    }

    LaunchedEffect(initializationError) {
        initializationError?.let { error ->
            updatedResult(
                BarcodeResult.OnFailed(
                    Exception(error),
                ),
            )
        }
    }

    scannerController?.onTorchChange = { enabled ->
        cameraControl?.enableTorch(enabled)
    }

    scannerController?.onZoomChange = { ratio ->
        cameraControl?.setZoomRatio(ratio)
    }

    val provider = cameraProvider

    provider?.let {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                val preview = Preview.Builder()
                    .build()

                val selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                preview.surfaceProvider = previewView.surfaceProvider

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                ),
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
                    )
                    .build()

                val barcodeAnalyzer = BarcodeAnalyzer(
                    getCamera = { camera },
                    codeTypes = codeTypes,
                    onSuccess = { scannedBarcodes ->
                        updatedResult(
                            BarcodeResult.OnSuccess(
                                scannedBarcodes.first(),
                            ),
                        )

                        provider.unbind(imageAnalysis)
                    },
                    onFailed = {
                        updatedResult(
                            BarcodeResult.OnFailed(
                                Exception(it),
                            ),
                        )
                    },
                    filter = filter,
                )

                analyzer = barcodeAnalyzer

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx), barcodeAnalyzer)

                camera = bindCamera(
                    lifecycleOwner = lifecycleOwner,
                    cameraProvider = provider,
                    selector = selector,
                    preview = preview,
                    imageAnalysis = imageAnalysis,
                    result = updatedResult,
                    cameraControl = { cameraControl = it },
                )

                previewView
            },
            onRelease = {
                provider.unbindAll()
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            analyzer?.close()
            analyzer = null
            camera = null
            cameraControl = null
            scannerController?.onTorchChange = null
            scannerController?.onZoomChange = null
        }
    }
}

internal fun bindCamera(
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    selector: CameraSelector,
    preview: Preview,
    imageAnalysis: ImageAnalysis,
    result: (BarcodeResult) -> Unit,
    cameraControl: (CameraControl?) -> Unit,
): Camera? = runCatching {
    cameraProvider.unbindAll()

    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        selector,
        preview,
        imageAnalysis,
    ).also {
        cameraControl(it.cameraControl)
    }
}.getOrElse {
    result(BarcodeResult.OnFailed(Exception(it)))
    null
}
