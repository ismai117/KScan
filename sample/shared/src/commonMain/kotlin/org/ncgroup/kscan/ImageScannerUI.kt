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
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.features.imagepicker.config.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.features.imagepicker.model.ImagePickerResult
import io.github.ismoy.imagepickerkmp.features.imagepicker.ui.rememberImagePickerKMP

@Composable
fun ImageScannerUI(modifier: Modifier = Modifier) {
    var barcode by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val picker = rememberImagePickerKMP(
        config = ImagePickerKMPConfig()
    )
    val pickerResult = picker.result

    LaunchedEffect(pickerResult) {
        when (pickerResult) {
            is ImagePickerResult.Success -> {
                val photo = pickerResult.photos.firstOrNull()
                if (photo != null) {
                    isScanning = true
                    errorMessage = ""
                    barcode = ""
                    format = ""

                    val imageBytes = photo.loadBytes()
                    scanImage(
                        imageBytes = imageBytes,
                        codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
                    ) { result ->
                        isScanning = false
                        when (result) {
                            is BarcodeResult.OnSuccess -> {
                                barcode = result.barcode.data
                                format = result.barcode.format
                            }
                            is BarcodeResult.OnFailed -> {
                                errorMessage = "Error: ${result.exception.message}"
                            }
                            BarcodeResult.OnCanceled -> {
                                // Not applicable for image scanning
                            }
                        }
                    }
                }
                picker.reset()
            }
            is ImagePickerResult.Error -> {
                errorMessage = "Error: ${pickerResult.exception.message}"
                picker.reset()
            }
            is ImagePickerResult.Dismissed -> {
                picker.reset()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            ModeSelector()
        }
    ) { padding ->
        Box(
            modifier = modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (barcode.isNotEmpty()) {
                    Text(text = "Data: $barcode")
                    Text(text = "Format: $format")
                }

                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = Color.Red)
                }

                when (pickerResult) {
                    is ImagePickerResult.Loading -> {
                        CircularProgressIndicator()
                        Text(text = "Loading image...")
                    }
                    else -> {
                        if (isScanning) {
                            CircularProgressIndicator()
                            Text(text = "Scanning...")
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { picker.launchCamera() },
                        enabled = !isScanning && pickerResult !is ImagePickerResult.Loading,
                    ) {
                        Text(text = "Camera")
                    }
                    Button(
                        onClick = { picker.launchGallery() },
                        enabled = !isScanning && pickerResult !is ImagePickerResult.Loading,
                    ) {
                        Text(text = "Gallery")
                    }
                }
            }
        }
    }
}
