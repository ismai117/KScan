package org.ncgroup.kscan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamPanel
import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.util.EnumMap
import kotlin.time.Duration.Companion.milliseconds

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
    var webcam by remember { mutableStateOf<Webcam?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        try {
            val defaultWebcam = Webcam.getDefault()

            if (defaultWebcam == null) {
                updatedResult(BarcodeResult.OnFailed(Exception("No webcam found")))

                return@DisposableEffect onDispose {}
            }

            defaultWebcam.viewSize = defaultWebcam.viewSizes.first()
            defaultWebcam.open()

            webcam = defaultWebcam
        } catch (e: Exception) {
            updatedResult(BarcodeResult.OnFailed(e))
        }

        val frameChannel = Channel<BufferedImage>(Channel.CONFLATED)

        val cameraJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                val image = webcam?.image ?: continue

                frameChannel.trySend(image)

                delay(30.milliseconds)
            }
        }

        val scannerJob = coroutineScope.launch(Dispatchers.Default) {
            val reader = MultiFormatReader()

            val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
            val formats = codeTypes.mapNotNull { it.toZxingFormat() }.ifEmpty {
                listOf(com.google.zxing.BarcodeFormat.QR_CODE)
            }
            hints[DecodeHintType.POSSIBLE_FORMATS] = formats
            reader.setHints(hints)

            for (image in frameChannel) {
                if (!isActive || !isScanning) break

                try {
                    val source = BufferedImageLuminanceSource(image)
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                    val result = reader.decode(binaryBitmap)

                    withContext(Dispatchers.Main) {
                        val barcode = Barcode(
                            data = result.text,
                            format = result.barcodeFormat.toKScanFormat().toString(),
                            rawBytes = result.rawBytes
                        )

                        if (filter(barcode)) {
                            withContext(Dispatchers.Main) {
                                isScanning = false
                                updatedResult(BarcodeResult.OnSuccess(barcode))
                            }
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
            webcam?.close()
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
        if (webcam != null) {
            SwingPanel(
                factory = {
                    WebcamPanel(webcam).apply {
                        isFPSDisplayed = false
                        isImageSizeDisplayed = false
                        isMirrored = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun BarcodeFormat.toZxingFormat(): com.google.zxing.BarcodeFormat? {
    return when (this) {
        BarcodeFormat.FORMAT_CODE_128 -> com.google.zxing.BarcodeFormat.CODE_128
        BarcodeFormat.FORMAT_CODE_39 -> com.google.zxing.BarcodeFormat.CODE_39
        BarcodeFormat.FORMAT_CODE_93 -> com.google.zxing.BarcodeFormat.CODE_93
        BarcodeFormat.FORMAT_CODABAR -> com.google.zxing.BarcodeFormat.CODABAR
        BarcodeFormat.FORMAT_EAN_13 -> com.google.zxing.BarcodeFormat.EAN_13
        BarcodeFormat.FORMAT_EAN_8 -> com.google.zxing.BarcodeFormat.EAN_8
        BarcodeFormat.FORMAT_ITF -> com.google.zxing.BarcodeFormat.ITF
        BarcodeFormat.FORMAT_UPC_A -> com.google.zxing.BarcodeFormat.UPC_A
        BarcodeFormat.FORMAT_UPC_E -> com.google.zxing.BarcodeFormat.UPC_E
        BarcodeFormat.FORMAT_QR_CODE -> com.google.zxing.BarcodeFormat.QR_CODE
        BarcodeFormat.FORMAT_PDF417 -> com.google.zxing.BarcodeFormat.PDF_417
        BarcodeFormat.FORMAT_AZTEC -> com.google.zxing.BarcodeFormat.AZTEC
        BarcodeFormat.FORMAT_DATA_MATRIX -> com.google.zxing.BarcodeFormat.DATA_MATRIX
        else -> null
    }
}

private fun com.google.zxing.BarcodeFormat.toKScanFormat(): BarcodeFormat {
    return when (this) {
        com.google.zxing.BarcodeFormat.CODE_128 -> BarcodeFormat.FORMAT_CODE_128
        com.google.zxing.BarcodeFormat.CODE_39 -> BarcodeFormat.FORMAT_CODE_39
        com.google.zxing.BarcodeFormat.CODE_93 -> BarcodeFormat.FORMAT_CODE_93
        com.google.zxing.BarcodeFormat.CODABAR -> BarcodeFormat.FORMAT_CODABAR
        com.google.zxing.BarcodeFormat.EAN_13 -> BarcodeFormat.FORMAT_EAN_13
        com.google.zxing.BarcodeFormat.EAN_8 -> BarcodeFormat.FORMAT_EAN_8
        com.google.zxing.BarcodeFormat.ITF -> BarcodeFormat.FORMAT_ITF
        com.google.zxing.BarcodeFormat.UPC_A -> BarcodeFormat.FORMAT_UPC_A
        com.google.zxing.BarcodeFormat.UPC_E -> BarcodeFormat.FORMAT_UPC_E
        com.google.zxing.BarcodeFormat.QR_CODE -> BarcodeFormat.FORMAT_QR_CODE
        com.google.zxing.BarcodeFormat.PDF_417 -> BarcodeFormat.FORMAT_PDF417
        com.google.zxing.BarcodeFormat.AZTEC -> BarcodeFormat.FORMAT_AZTEC
        com.google.zxing.BarcodeFormat.DATA_MATRIX -> BarcodeFormat.FORMAT_DATA_MATRIX
        else -> BarcodeFormat.FORMAT_ALL_FORMATS
    }
}
