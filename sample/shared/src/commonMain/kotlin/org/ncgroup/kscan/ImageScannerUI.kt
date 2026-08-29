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
    var picked by remember { mutableStateOf("") }

    val picker = rememberImagePickerKMP(config = ImagePickerKMPConfig())
    val pickerResult = picker.result

    LaunchedEffect(pickerResult) {
        when (pickerResult) {
            is ImagePickerResult.Success -> {
                pickerResult.photos.firstOrNull()?.let { photo ->
                    val raw = photo.loadBytes()
                    picked = describeBytes(raw)
                    barcode = ""
                    format = ""
                    error = ""
                    isScanning = true

                    scanImage(
                        imageBytes = imageBytesOf(raw),
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

                if (picked.isNotEmpty()) {
                    Text(text = picked)
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
 * Returns the picked image's bytes, whatever shape the picker handed back.
 *
 * On web imagepickerkmp reads files with FileReader.readAsDataURL, so what
 * arrives is the text of a data URL rather than an image. Other platforms return
 * the bytes already, and are recognised as such and passed through.
 */
@OptIn(ExperimentalEncodingApi::class)
private fun imageBytesOf(picked: ByteArray): ByteArray {
    val head = picked.take(64).toByteArray().decodeToString()

    val encoded = when {
        // "data:image/png;base64,iVBOR..."
        head.startsWith("data:") -> picked.decodeToString().substringAfter("base64,", "")

        // bare base64, with no data URL wrapping it
        head.isNotEmpty() && head.all { it.isBase64Character() } -> picked.decodeToString()

        // already an image
        else -> ""
    }

    if (encoded.isEmpty()) return picked

    return runCatching { Base64.decode(encoded) }.getOrDefault(picked)
}

private fun Char.isBase64Character(): Boolean = isLetterOrDigit() || this == '+' || this == '/' || this == '=' || this == '\n' || this == '\r'

/** What the picker handed back, so a failure can be diagnosed on the device. */
private fun describeBytes(bytes: ByteArray): String {
    val hex = bytes.take(8).joinToString(" ") {
        val v = it.toInt() and 0xFF
        v.toString(16).padStart(2, '0')
    }
    val text = bytes.take(24).toByteArray().decodeToString()
        .map { if (it.code in 32..126) it else '.' }
        .joinToString("")

    return "picked ${bytes.size} bytes | $hex | $text"
}
