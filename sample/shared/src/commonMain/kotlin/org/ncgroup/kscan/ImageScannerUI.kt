package org.ncgroup.kscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.ismoy.imagepickerkmp.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.picker.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.picker.ImagePickerResult
import io.github.ismoy.imagepickerkmp.picker.rememberImagePickerKMP
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Scanning a barcode out of a still image rather than the camera feed. */
@Composable
fun ImageScannerUI(modifier: Modifier = Modifier) {
    var barcode by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    val picker = rememberImagePickerKMP(config = ImagePickerKMPConfig())
    val pickerResult = picker.result

    LaunchedEffect(pickerResult) {
        when (pickerResult) {
            is ImagePickerResult.Success -> {
                pickerResult.photos.firstOrNull()?.let { photo ->
                    barcode = ""
                    format = ""
                    error = ""
                    isScanning = true

                    scanImage(
                        imageBytes = imageBytesOf(photo.loadBytes()),
                        codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
                    ) { result ->
                        isScanning = false
                        when (result) {
                            is BarcodeResult.OnSuccess -> {
                                barcode = result.barcode.data
                                format = result.barcode.format
                            }

                            is BarcodeResult.OnFailed -> {
                                error = "Error: ${result.exception.message}"
                            }

                            BarcodeResult.OnCanceled -> Unit
                        }
                    }
                }
                picker.reset()
            }

            is ImagePickerResult.Error -> {
                error = "Error: ${pickerResult.exception.message}"
                picker.reset()
            }

            is ImagePickerResult.Dismissed -> picker.reset()

            else -> Unit
        }
    }

    val busy = isScanning || pickerResult is ImagePickerResult.Loading

    Scaffold(modifier = modifier, topBar = { ModeSelector() }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (barcode.isNotEmpty()) {
                    Text(text = "Data: $barcode")
                    Text(text = "Format: $format")
                }

                if (error.isNotEmpty()) {
                    Text(text = error, color = Color.Red)
                }

                if (busy) {
                    CircularProgressIndicator()
                    Text(text = if (isScanning) "Scanning..." else "Loading image...")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { picker.launchCamera() }, enabled = !busy) {
                        Text(text = "Camera")
                    }
                    Button(onClick = { picker.launchGallery() }, enabled = !busy) {
                        Text(text = "Gallery")
                    }
                }
            }
        }
    }
}

/**
 * Returns the picked image's bytes.
 *
 * On web the picker reads files with FileReader.readAsDataURL and hands back
 * the text of that data URL rather than the image, so the payload is decoded
 * here. Other platforms already return the bytes and are left alone.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun imageBytesOf(bytes: ByteArray): ByteArray {
    val prefix = bytes.take(5).toByteArray().decodeToString()
    if (prefix != "data:") return bytes

    val text = bytes.decodeToString()
    val payload = text.substringAfter("base64,", missingDelimiterValue = "")

    return if (payload.isEmpty()) bytes else Base64.decode(payload)
}
