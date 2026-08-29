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
import androidx.compose.ui.unit.dp
import io.github.ismoy.imagepickerkmp.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.picker.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.picker.ImagePickerResult
import io.github.ismoy.imagepickerkmp.picker.rememberImagePickerKMP

/** Scans a barcode out of an image taken with the camera or picked from the gallery. */
@Composable
fun ImageScannerUI(modifier: Modifier = Modifier) {
    val scan = remember { ScanState() }
    var isScanning by remember { mutableStateOf(false) }

    val picker = rememberImagePickerKMP(config = ImagePickerKMPConfig())
    val pickerResult = picker.result

    LaunchedEffect(pickerResult) {
        when (pickerResult) {
            is ImagePickerResult.Success -> {
                pickerResult.photos.firstOrNull()?.let { photo ->
                    scan.clear()
                    isScanning = true
                    scanImage(
                        imageBytes = photo.loadBytes(),
                        codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
                    ) { result ->
                        isScanning = false
                        scan.accept(result)
                    }
                }
                picker.reset()
            }

            is ImagePickerResult.Error -> {
                scan.accept(BarcodeResult.OnFailed(pickerResult.exception))
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
                ScanResult(scan)

                if (busy) {
                    CircularProgressIndicator()
                    Text(text = if (isScanning) "Scanning..." else "Loading image...")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = picker::launchCamera, enabled = !busy) {
                        Text(text = "Camera")
                    }
                    Button(onClick = picker::launchGallery, enabled = !busy) {
                        Text(text = "Gallery")
                    }
                }
            }
        }
    }
}
