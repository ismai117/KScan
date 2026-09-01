package org.ncgroup.kscan.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.CameraDevice
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.ScannerView
import org.ncgroup.kscan.camera.platformCameras
import org.ncgroup.kscan.camera.supportsCameraListing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    format: BarcodeFormat,
    state: ScannerState,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scannerController = remember { ScannerController() }
    var error by remember { mutableStateOf("") }
    var pickingCamera by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            state.scanned = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = format.name.removePrefix("FORMAT_"))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (supportsCameraListing) {
                        IconButton(onClick = { pickingCamera = true }) {
                            Icon(
                                imageVector = Icons.Filled.Cameraswitch,
                                contentDescription = "Camera",
                            )
                        }
                    }

                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val scanned = state.scanned

                if (pickingCamera) {
                    CameraDialog(
                        selected = state.cameraId,
                        onPick = { id ->
                            state.cameraId = id
                            pickingCamera = false
                        },
                        onDismiss = { pickingCamera = false },
                    )
                } else if (scanned == null) {
                    ScannerView(
                        codeTypes = listOf(format),
                        modifier = Modifier.matchParentSize(),
                        cameraId = state.cameraId,
                        scannerController = scannerController,
                        filter = { barcode -> state.accepts(barcode) },
                        autoZoom = state.autoZoom,
                    ) { result ->
                        when (result) {
                            is BarcodeResult.OnSuccess -> {
                                state.scanned = result.barcode
                                error = ""
                            }

                            is BarcodeResult.OnFailed -> {
                                error = "Error: ${result.exception.message}"
                            }
                        }
                    }
                } else {
                    Result(
                        barcode = scanned,
                        onScanAgain = { state.scanned = null },
                    )
                }
            }

            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (scannerController.maxZoomRatio > 1f) {
                Text(
                    text = "Zoom ${scannerController.zoomRatio}x",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                )

                Slider(
                    value = scannerController.zoomRatio,
                    onValueChange = { scannerController.setZoom(it) },
                    valueRange = 1f..scannerController.maxZoomRatio,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = { scannerController.setTorch(!scannerController.torchEnabled) },
                ) {
                    Text(text = if (scannerController.torchEnabled) "Torch on" else "Torch off")
                }
            }
        }
    }
}

// The preview is gone while this is up: listing opens every camera in turn, and
// the one the scanner is holding would not answer.
@Composable
private fun CameraDialog(
    selected: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var cameras by remember { mutableStateOf(emptyList<CameraDevice>()) }
    var searching by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        cameras = platformCameras()
        searching = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Camera") },
        text = {
            Column {
                when {
                    searching -> Text(text = "Looking\u2026")

                    cameras.isEmpty() -> Text(text = "No camera found.")

                    else -> {
                        CameraChoice(
                            label = "Default",
                            selected = selected == null,
                            onClick = { onPick(null) },
                        )

                        cameras.forEach { device ->
                            CameraChoice(
                                label = device.label.ifEmpty { "Camera ${device.id}" },
                                selected = selected == device.id,
                                onClick = { onPick(device.id) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        },
    )
}

@Composable
private fun CameraChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)

        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Result(
    barcode: Barcode,
    onScanAgain: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "DATA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(text = barcode.data, style = MaterialTheme.typography.bodyLarge)

                Text(
                    text = "FORMAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = barcode.format.name.removePrefix("FORMAT_"),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        FilledTonalButton(onClick = onScanAgain) {
            Text(text = "Scan again")
        }
    }
}
