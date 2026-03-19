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
    var grabber by remember { mutableStateOf<OpenCVFrameGrabber?>(null) }
    var cameraFrameBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val defaultGrabber = OpenCVFrameGrabber(0)

                defaultGrabber.imageWidth = 1920
                defaultGrabber.imageHeight = 1080
                defaultGrabber.setVideoOption("focus_auto", "1")

                defaultGrabber.start()

                grabber = defaultGrabber
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updatedResult(BarcodeResult.OnFailed(e))
                }
            }
        }

        onDispose {
            coroutineScope.launch(Dispatchers.IO) {
                grabber?.stop()
                grabber?.release()
                grabber = null
            }
        }
    }

    if (grabber != null) {
        DisposableEffect(Unit) {
            val converter = Java2DFrameConverter()
            val frameChannel = Channel<BufferedImage>(Channel.CONFLATED)

            val poolSize = 5
            val imagePool = Array<BufferedImage?>(poolSize) { null }
            var poolIndex = 0

            val cameraJob = coroutineScope.launch(Dispatchers.IO) {
                while (isActive && isScanning) {
                    try {
                        val frame = grabber?.grab() ?: continue
                        val image = converter.convert(frame)

                        if (imagePool[poolIndex] == null) {
                            imagePool[poolIndex] = BufferedImage(
                                image.width,
                                image.height,
                                image.type
                            )
                        }

                        val targetImage = imagePool[poolIndex]!!

                        val graphics = targetImage.graphics
                        graphics.drawImage(image, 0, 0, null)
                        graphics.dispose()

                        frameChannel.trySend(image)

                        coroutineScope.launch(Dispatchers.Default) {
                            val composeBitmap = targetImage.toComposeImageBitmap()

                            cameraFrameBitmap = composeBitmap
                        }

                        poolIndex = (poolIndex + 1) % poolSize
                    } catch (_: org.bytedeco.javacv.FrameGrabber.Exception) {
                        continue
                    }
                }
            }

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
                        } else { null } ?: result.text.toByteArray(Charsets.ISO_8859_1)

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

            onDispose {
                cameraJob.cancel()
                scannerJob.cancel()
                frameChannel.close()
            }
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
