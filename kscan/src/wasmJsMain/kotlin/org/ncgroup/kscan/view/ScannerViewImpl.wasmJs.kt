package org.ncgroup.kscan.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import org.ncgroup.kscan.scanner.RepeatedDetection
import org.ncgroup.kscan.scanner.applyTorch
import org.ncgroup.kscan.scanner.applyZoom
import org.ncgroup.kscan.scanner.barcodeDetector
import org.ncgroup.kscan.scanner.cameraCapabilities
import org.ncgroup.kscan.scanner.createVideoElement
import org.ncgroup.kscan.scanner.detectFromVideoFrame
import org.ncgroup.kscan.scanner.firstMatching
import org.ncgroup.kscan.scanner.freezeCamera
import org.ncgroup.kscan.scanner.isVideoReady
import org.ncgroup.kscan.scanner.startCamera
import org.ncgroup.kscan.scanner.stopCamera
import org.ncgroup.kscan.scanner.toList
import kotlin.time.Duration.Companion.milliseconds

private const val SCAN_INTERVAL_MS = 33L

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
@Composable
internal actual fun ScannerViewImpl(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    autoZoom: Boolean,
    result: (BarcodeResult) -> Unit,
) {
    val updatedResult by rememberUpdatedState(result)
    val updatedFilter by rememberUpdatedState(filter)
    val coroutineScope = rememberCoroutineScope()

    val video = remember { createVideoElement() }
    var isScanning by remember { mutableStateOf(true) }

    scannerController?.onTorchChange = remember {
        { enabled: Boolean ->
            coroutineScope.launch {
                try {
                    applyTorch(video, enabled).await()
                    scannerController.torchEnabled = enabled
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

    LaunchedEffect(codeTypes) {
        try {
            startCamera(
                video = video,
                deviceId = KScanWeb.cameraDeviceId.orEmpty(),
                debug = KScanWeb.debugLogging,
            ).await()

            val capabilities = cameraCapabilities(video)
            scannerController?.maxZoomRatio =
                capabilities.maxZoomRatio.toFloat().coerceAtLeast(1f)

            val repeated = RepeatedDetection()

            val detector = barcodeDetector(
                formats = BarcodeFormatMapper.toWebFormats(codeTypes),
                polyfillUrl = KScanWeb.barcodeDetectorPolyfillUrl.orEmpty(),
                zxingWasmUrl = KScanWeb.zxingWasmUrl.orEmpty(),
                debug = KScanWeb.debugLogging,
            )

            while (isActive && isScanning) {
                if (isVideoReady(video)) {
                    val detected =
                        detectFromVideoFrame(detector, video, KScanWeb.debugLogging)
                            .await()
                            .toList()

                    val matchingBarcode = detected.firstMatching(codeTypes, updatedFilter)

                    if (matchingBarcode != null && repeated.accept(matchingBarcode.data)) {
                        isScanning = false
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
            isScanning = false
            stopCamera(video)
            scannerController?.onTorchChange = null
            scannerController?.onZoomChange = null
        }
    }
}
