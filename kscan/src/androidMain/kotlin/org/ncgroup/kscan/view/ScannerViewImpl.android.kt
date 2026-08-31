package org.ncgroup.kscan.view

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.scanner.BarcodeAnalyzer
import org.ncgroup.kscan.scanner.MAX_ZOOM_RATIO
import org.ncgroup.kscan.scanner.barcodeScannerOptions

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
    var frozenFrame: ImageBitmap? by remember { mutableStateOf(null) }

    val updatedResult by rememberUpdatedState(result)
    val updatedFilter by rememberUpdatedState(filter)

    DisposableEffect(camera, scannerController) {
        val cameraInfo = camera?.cameraInfo

        val torchObserver = Observer<Int> { state ->
            scannerController?.torchEnabled = state == TorchState.ON
        }

        val zoomObserver = Observer<ZoomState> { state ->
            scannerController?.zoomRatio = state.zoomRatio
            scannerController?.maxZoomRatio = state.maxZoomRatio.coerceAtMost(MAX_ZOOM_RATIO)
        }

        cameraInfo?.torchState?.observe(lifecycleOwner, torchObserver)
        cameraInfo?.zoomState?.observe(lifecycleOwner, zoomObserver)

        onDispose {
            cameraInfo?.torchState?.removeObserver(torchObserver)
            cameraInfo?.zoomState?.removeObserver(zoomObserver)
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

    val previewView = remember { PreviewView(context) }

    val preview = remember {
        Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
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
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    // Both the decoder's formats and auto zoom are fixed when the analyzer is
    // built, so a caller changing either gets a new one bound to the same preview.
    DisposableEffect(provider, codeTypes, autoZoom) {
        val barcodeAnalyzer = provider?.let {
            BarcodeAnalyzer(
                codeTypes = codeTypes,
                scannerOptions = barcodeScannerOptions(
                    codeTypes = codeTypes,
                    autoZoom = autoZoom,
                    getCamera = { camera },
                ),
                onSuccess = { scannedBarcodes ->
                    frozenFrame = previewView.bitmap?.asImageBitmap()
                    provider.unbindAll()

                    updatedResult(BarcodeResult.OnSuccess(scannedBarcodes.first()))
                },
                onFailed = { updatedResult(BarcodeResult.OnFailed(Exception(it))) },
                filter = { barcode -> updatedFilter(barcode) },
            )
        }

        if (provider != null && barcodeAnalyzer != null) {
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                barcodeAnalyzer,
            )

            camera = bindCamera(
                lifecycleOwner = lifecycleOwner,
                cameraProvider = provider,
                selector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build(),
                preview = preview,
                imageAnalysis = imageAnalysis,
                result = updatedResult,
                cameraControl = { cameraControl = it },
            )
        }

        onDispose {
            imageAnalysis.clearAnalyzer()
            barcodeAnalyzer?.close()
            provider?.unbindAll()

            camera = null
            cameraControl = null
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { previewView },
        )

        // Scaled like PreviewView's FILL_CENTER so the held frame lands where the
        // live one was.
        frozenFrame?.let { frame ->
            Image(
                bitmap = frame,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }

    DisposableEffect(scannerController) {
        onDispose {
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
