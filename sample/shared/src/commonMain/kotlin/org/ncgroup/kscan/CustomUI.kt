package org.ncgroup.kscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
fun CustomUI(modifier: Modifier = Modifier) {
    var showScanner by remember { mutableStateOf(false) }
    var barcode by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val scannerController = remember { ScannerController() }

    Scaffold(
        modifier = modifier,
        topBar = { if (!showScanner) ModeSelector() },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                if (barcode.isNotEmpty()) {
                    Text(text = "Data: $barcode")
                    Text(text = "Format: $format")
                }

                if (error.isNotEmpty()) {
                    Text(text = error, color = Color.Red)
                }

                Button(onClick = { showScanner = true }) {
                    Text(text = "Scan Barcode")
                }
            }

            if (showScanner) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                scannerController.setTorch(!scannerController.torchEnabled)
                            },
                        ) {
                            Text(
                                text = "Torch ${if (scannerController.torchEnabled) "On" else "Off"}",
                            )
                        }

                        Button(onClick = { showScanner = false }) {
                            Text(text = "Close")
                        }
                    }

                    Text(
                        text = "Zoom: ${scannerController.zoomRatio}",
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Slider(
                        value = scannerController.zoomRatio,
                        onValueChange = { ratio -> scannerController.setZoom(ratio) },
                        valueRange = 1f..scannerController.maxZoomRatio,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    )

                    ScannerView(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
                        scannerController = scannerController,
                    ) { result ->
                        when (result) {
                            is BarcodeResult.OnSuccess -> {
                                barcode = result.barcode.data
                                format = result.barcode.format
                                error = ""
                            }

                            is BarcodeResult.OnFailed -> {
                                error = "Error: ${result.exception.message}"
                            }

                            BarcodeResult.OnCanceled -> Unit
                        }
                        showScanner = false
                    }
                }
            }
        }
    }
}
