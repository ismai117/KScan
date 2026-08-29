package org.ncgroup.kscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ImageScannerUI(modifier: Modifier = Modifier) {
    var barcode by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    val pickImage = rememberImagePicker { imageBytes ->
        barcode = ""
        format = ""
        error = ""
        isScanning = true

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
                    error = "Error: ${result.exception.message}"
                }

                BarcodeResult.OnCanceled -> Unit
            }
        }
    }

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

                if (isScanning) {
                    CircularProgressIndicator()
                    Text(text = "Scanning...")
                }

                Button(onClick = pickImage, enabled = !isScanning) {
                    Text(text = "Pick image")
                }
            }
        }
    }
}
