package org.ncgroup.kscan

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
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
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameGrabber
import java.awt.image.BufferedImage
import java.util.EnumMap

@Composable
actual fun ScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    colors: ScannerColors,
    scannerUiOptions: ScannerUiOptions?,
    scannerController: ScannerController?,
    filter: (Barcode) -> Boolean,
    result: (BarcodeResult) -> Unit
) {
    val updatedResult by rememberUpdatedState(result)
    val coroutineScope = rememberCoroutineScope()
    var cameraFrameBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val frameChannel = Channel<BufferedImage>(Channel.CONFLATED)

        val scannerJob = coroutineScope.launch(Dispatchers.Default) {
            val reader = MultiFormatReader()
            val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)

            val formats = codeTypes.mapNotNull { it.toZxingFormat() }.ifEmpty {
                listOf(com.google.zxing.BarcodeFormat.QR_CODE)
            }

            hints[DecodeHintType.POSSIBLE_FORMATS] = formats
            hints[DecodeHintType.CHARACTER_SET] = "ISO-8859-1"
            hints[DecodeHintType.TRY_HARDER] = true

            reader.setHints(hints)

            var rgbPixels: IntArray? = null
            var fastSource: FastLuminanceSource? = null

            for (image in frameChannel) {
                if (!isActive || !isScanning) break

                try {
                    val width = image.width
                    val height = image.height

                    if (rgbPixels == null || rgbPixels.size != width * height) {
                        rgbPixels = IntArray(width * height)
                        fastSource = FastLuminanceSource(width, height)
                    }

                    image.getRGB(0, 0, width, height, rgbPixels, 0, width)

                    val luminances = fastSource!!.luminances

                    for (i in rgbPixels.indices) {
                        val pixel = rgbPixels[i]
                        val r = (pixel shr 16) and 0xff
                        val g = (pixel shr 8) and 0xff
                        val b = pixel and 0xff

                        luminances[i] = ((r + (g shl 1) + b) shr 2).toByte()
                    }

                    val binaryBitmap = BinaryBitmap(HybridBinarizer(fastSource))
                    val result = reader.decodeWithState(binaryBitmap)

                    val bytes = if (result.resultMetadata.containsKey(ResultMetadataType.BYTE_SEGMENTS)) {
                        val byteSegments = result.resultMetadata[ResultMetadataType.BYTE_SEGMENTS] as? MutableList<ByteArray?>

                        byteSegments?.firstOrNull() ?: byteArrayOf()
                    } else {
                        null
                    } ?: result.text.toByteArray(Charsets.ISO_8859_1)

                    withContext(Dispatchers.Main) {
                        val barcode = Barcode(
                            data = result.text,
                            format = result.barcodeFormat.toKScanFormat().toString(),
                            rawBytes = bytes
                        )

                        if (filter(barcode)) {
                            isScanning = false
                            updatedResult(BarcodeResult.OnSuccess(barcode))
                        }
                    }
                } catch (_: NotFoundException) {
                    // no barcode found -> next image
                } catch (e: Exception) {
                    updatedResult(BarcodeResult.OnFailed(e))
                }
            }
        }

        val cameraJob = coroutineScope.launch(Dispatchers.IO) {
            var localGrabber: OpenCVFrameGrabber? = null

            try {
                localGrabber = OpenCVFrameGrabber(0).apply {
                    imageWidth = 1920
                    imageHeight = 1080
                    setVideoOption("focus_auto", "1")
                    start()
                }

                val converter = Java2DFrameConverter()

                while (isActive && isScanning) {
                    try {
                        val frame = localGrabber.grab() ?: continue
                        val image = converter.convert(frame)

                        frameChannel.trySend(image)
                    } catch (_: org.bytedeco.javacv.FrameGrabber.Exception) {
                        continue
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updatedResult(BarcodeResult.OnFailed(e))
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
            frameChannel.consumeAsFlow().collectLatest { image ->
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
            frameChannel.close()
        }
    }

    ScannerViewContent(
        modifier = modifier,
        colors = colors,
        scannerUiOptions = scannerUiOptions,
        torchEnabled = false,
        onTorchEnabled = {},
        zoomRatio = 1f,
        onZoomChange = {},
        maxZoomRatio = 1f,
        onCancel = {
            isScanning = false
            updatedResult(BarcodeResult.OnCanceled)
        }
    ) {
        cameraFrameBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "Camera Feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

class FastLuminanceSource(
    width: Int,
    height: Int
) : com.google.zxing.LuminanceSource(width, height) {
    val luminances = ByteArray(width * height)

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val width = width
        val res = if (row == null || row.size < width) ByteArray(width) else row
        System.arraycopy(luminances, y * width, res, 0, width)
        return res
    }

    override fun getMatrix(): ByteArray = luminances
}
