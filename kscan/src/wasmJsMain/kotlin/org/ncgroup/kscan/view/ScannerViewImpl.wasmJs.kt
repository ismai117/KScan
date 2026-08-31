package org.ncgroup.kscan.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.KScanWeb
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.format.BarcodeFormatMapper
import org.ncgroup.kscan.format.firstMatching
import org.ncgroup.kscan.scanner.MAX_ZOOM_RATIO
import org.ncgroup.kscan.scanner.RepeatedDetection
import org.ncgroup.kscan.scanner.applyTorch
import org.ncgroup.kscan.scanner.applyZoom
import org.ncgroup.kscan.scanner.barcodeDetector
import org.ncgroup.kscan.scanner.cameraCapabilities
import org.ncgroup.kscan.scanner.createVideoElement
import org.ncgroup.kscan.scanner.detectFromVideoFrame
import org.ncgroup.kscan.scanner.freezeCamera
import org.ncgroup.kscan.scanner.isVideoReady
import org.ncgroup.kscan.scanner.startCamera
import org.ncgroup.kscan.scanner.stopCamera
import org.ncgroup.kscan.scanner.toBarcodes
import kotlin.time.Duration.Companion.milliseconds

private const val SCAN_INTERVAL_MS = 33L

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
@Composable
internal actual fun ScannerViewImpl(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier,
    cameraId: String?,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    autoZoom: Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val updatedResult by rememberUpdatedState(result)
    val updatedFilter by rememberUpdatedState(filter)
    val coroutineScope = rememberCoroutineScope()

    val video = remember { createVideoElement() }

    scannerController?.onTorchChange = remember {
        { enabled: Boolean ->
            coroutineScope.launch {
                try {
                    val applied = applyTorch(video, enabled).await().toBoolean()
                    scannerController.torchEnabled = enabled && applied
                } catch (e: Throwable) {
                    updatedResult(
                        BarcodeResult.OnFailed(Exception(e.message ?: "Torch toggle failed")),
                    )
                }
            }
        }
    }

    scannerController?.onZoomChange = remember {
        { ratio: Float ->
            scannerController.zoomRatio = ratio
            coroutineScope.launch { applyZoom(video, ratio.toDouble()).await() }
        }
    }

    LaunchedEffect(codeTypes, cameraId) {
        try {
            startCamera(
                video = video,
                deviceId = cameraId.orEmpty(),
                debug = KScanWeb.debugLogging,
            ).await()

            val capabilities = cameraCapabilities(video)
            scannerController?.maxZoomRatio =
                capabilities.maxZoomRatio.toFloat().coerceIn(1f, MAX_ZOOM_RATIO)

            val repeated = RepeatedDetection()

            val detector = barcodeDetector(
                formats = BarcodeFormatMapper.toWebFormats(codeTypes),
                polyfillUrl = KScanWeb.barcodeDetectorPolyfillUrl.orEmpty(),
                zxingWasmUrl = KScanWeb.zxingWasmUrl.orEmpty(),
                debug = KScanWeb.debugLogging,
            )

            while (isActive) {
                if (isVideoReady(video)) {
                    val matchingBarcode =
                        detectFromVideoFrame(detector, video, KScanWeb.debugLogging)
                            .await()
                            .toBarcodes()
                            .firstMatching(codeTypes, updatedFilter)

                    if (matchingBarcode != null && repeated.accept(matchingBarcode.data)) {
                        freezeCamera(video)
                        updatedResult(BarcodeResult.OnSuccess(matchingBarcode))
                        break
                    }
                }
                delay(SCAN_INTERVAL_MS.milliseconds)
            }
        } catch (e: Throwable) {
            updatedResult(BarcodeResult.OnFailed(Exception(e.message ?: e.toString())))
        }
    }

    HtmlElementView(
        factory = { video },
        modifier = modifier,
    )

    DisposableEffect(Unit) {
        onDispose {
            stopCamera(video)
            scannerController?.onTorchChange = null
            scannerController?.onZoomChange = null
        }
    }
}
