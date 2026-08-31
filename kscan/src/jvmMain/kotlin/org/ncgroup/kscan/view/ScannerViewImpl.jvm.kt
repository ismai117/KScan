package org.ncgroup.kscan.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameGrabber
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.KScanDesktop
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.scanner.GrayLuminanceSource
import org.ncgroup.kscan.scanner.RepeatedDetection
import org.ncgroup.kscan.scanner.openCamera
import org.ncgroup.kscan.scanner.toBarcode
import org.ncgroup.kscan.scanner.toBinaryBitmap
import org.ncgroup.kscan.scanner.zxingReader
import java.awt.image.BufferedImage

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
    var cameraFrameBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        // A frame has two independent readers, so each gets its own channel. Sharing
        // one would hand every frame to whichever happened to receive it first,
        // leaving the decoder to work from half of them.
        val scanChannel = Channel<BufferedImage>(Channel.CONFLATED)
        val previewChannel = Channel<BufferedImage>(Channel.CONFLATED)

        val scannerJob = coroutineScope.launch(Dispatchers.Default) {
            val reader = zxingReader(codeTypes)
            val repeated = RepeatedDetection()

            var rgbPixels: IntArray? = null
            var source: GrayLuminanceSource? = null

            for (image in scanChannel) {
                if (!isActive || !isScanning) break

                try {
                    val width = image.width
                    val height = image.height

                    if (rgbPixels == null || rgbPixels.size != width * height) {
                        rgbPixels = IntArray(width * height)
                        source = GrayLuminanceSource(width, height)
                    }

                    val result = reader.decodeWithState(image.toBinaryBitmap(rgbPixels, source!!))

                    if (!repeated.accept(result.text)) continue

                    withContext(Dispatchers.Main) {
                        val barcode = result.toBarcode()

                        if (updatedFilter(barcode)) {
                            isScanning = false
                            updatedResult(BarcodeResult.OnSuccess(barcode))
                        }
                    }
                } catch (_: NotFoundException) {
                    // A frame holding no barcode breaks the run of sightings, so a
                    // stray misread cannot pair up with a later one and be reported.
                    repeated.reset()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updatedResult(BarcodeResult.OnFailed(e))
                    }
                }
            }
        }

        val cameraJob = coroutineScope.launch(Dispatchers.IO) {
            var localGrabber: OpenCVFrameGrabber? = null

            try {
                localGrabber = openCamera(KScanDesktop.cameraIndex)

                val converter = Java2DFrameConverter()

                while (isActive && isScanning) {
                    try {
                        val frame = localGrabber.grab() ?: continue
                        val image = converter.convert(frame)

                        scanChannel.trySend(image)
                        previewChannel.trySend(image)
                    } catch (_: org.bytedeco.javacv.FrameGrabber.Exception) {
                        continue
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updatedResult(
                        BarcodeResult.OnFailed(
                            Exception(
                                "No camera could be opened. Check that one is " +
                                    "connected and that nothing else is using it.",
                                e,
                            ),
                        ),
                    )
                }
            } finally {
                try {
                    localGrabber?.stop()
                    localGrabber?.release()
                } catch (_: Exception) {
                    // ignore exceptions on release
                }
            }
        }

        val uiUpdateJob = coroutineScope.launch(Dispatchers.Default) {
            previewChannel.consumeAsFlow().collectLatest { image ->
                val composeBitmap = image.toComposeImageBitmap()

                withContext(Dispatchers.Main) {
                    cameraFrameBitmap = composeBitmap
                }
            }
        }

        onDispose {
            isScanning = false

            cameraJob.cancel()
            scannerJob.cancel()
            uiUpdateJob.cancel()
            scanChannel.close()
            previewChannel.close()
        }
    }

    cameraFrameBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = "Camera Feed",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
