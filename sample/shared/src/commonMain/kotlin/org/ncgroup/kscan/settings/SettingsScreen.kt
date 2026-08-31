package org.ncgroup.kscan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ncgroup.kscan.CameraDevice
import org.ncgroup.kscan.camera.platformCameras
import org.ncgroup.kscan.camera.supportsCameraListing
import org.ncgroup.kscan.scanner.ScannerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: ScannerState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingRow(
                label = "Auto zoom",
                description = "Let the decoder zoom in on a barcode too small to read",
            ) {
                Switch(
                    checked = state.autoZoom,
                    onCheckedChange = { state.autoZoom = it },
                )
            }

            SettingRow(
                label = "Filter",
                description = "Report only barcodes starting with the text below",
            ) {
                Switch(
                    checked = state.filterEnabled,
                    onCheckedChange = { state.filterEnabled = it },
                )
            }

            if (state.filterEnabled) {
                OutlinedTextField(
                    value = state.filterPrefix,
                    onValueChange = { state.filterPrefix = it },
                    label = { Text(text = "Starts with") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            CameraPicker(state = state)
        }
    }
}

@Composable
private fun CameraPicker(state: ScannerState) {
    // Listing opens each camera in turn on desktop, so it is asked for once.
    var cameras by remember { mutableStateOf(emptyList<CameraDevice>()) }
    var searching by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        cameras = platformCameras()
        searching = false
    }

    if (!supportsCameraListing) return

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = "Camera", style = MaterialTheme.typography.bodyLarge)

        Text(
            text = "Which camera the scanner opens",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (searching) {
            Text(
                text = "Looking\u2026",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            return@Column
        }

        if (cameras.isEmpty()) {
            Text(
                text = "None found. A camera the scanner still holds is not listed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )

            return@Column
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.cameraId == null,
                onClick = { state.cameraId = null },
                label = { Text(text = "Default") },
            )

            cameras.forEachIndexed { index, device ->
                FilterChip(
                    selected = state.cameraId == device.id,
                    onClick = { state.cameraId = device.id },
                    label = { Text(text = device.label.ifEmpty { "Camera $index" }) },
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        control()
    }
}
