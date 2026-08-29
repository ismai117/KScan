@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ncgroup.kscan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * How long to wait between detection passes over the live preview.
 *
 * The camera is asked for 30fps, so sampling faster than a frame arrives is
 * wasted. A pass costs about 10ms with a native detector and about 33ms with the
 * polyfill, so this stays well inside the frame budget.
 */
private const val SCAN_INTERVAL_MS = 33L

/**
 * Web draws the camera preview only: [colors] and [scannerUiOptions] are accepted
 * for source compatibility but nothing is rendered over the preview yet.
 *
 * The preview is an HTML element composed into the scene, and the browser stacks
 * it above the canvas Compose draws into, so overlay content is not reliably
 * visible. Everything else behaves as it does on the other platforms:
 *
 * - [scannerController] drives torch and zoom, since those act on the camera
 *   track rather than on the DOM, and reports [ScannerController.maxZoomRatio]
 *   so a caller can bound its own zoom control.
 * - [filter] and [result] are unchanged.
 *
 * Build your own controls around `ScannerView` and drive them through
 * [scannerController]. Keep them outside the preview's bounds: an interop element
 * also consumes input events over its own area.
 *
 * Once overlay rendering is possible on web, [ScannerViewContent] can be restored
 * here and this file behaves like the other platforms with no API change.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
public actual fun ScannerView(
    codeTypes: List<BarcodeFormat>,
    modifier: Modifier,
    colors: ScannerColors,
    scannerUiOptions: ScannerUiOptions?,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
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
            Unit
        }
    }

    scannerController?.onZoomChange = remember {
        { ratio: Float ->
            scannerController.zoomRatio = ratio
            coroutineScope.launch { applyZoom(video, ratio.toDouble()).await() }
            Unit
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

                    if (matchingBarcode != null) {
                        isScanning = false
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

    Box(modifier = modifier) {
        HtmlElementView(
            factory = { video },
            modifier = Modifier.fillMaxSize(),
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            isScanning = false
            stopCamera(video)
        }
    }
}
