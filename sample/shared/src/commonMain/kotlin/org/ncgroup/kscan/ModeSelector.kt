package org.ncgroup.kscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ModeSelector(modifier: Modifier = Modifier) {
    val modeState = LocalScannerModeState.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarPadding.calculateTopPadding() + 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        FilterChip(
            selected = modeState.mode == ScannerMode.Default,
            onClick = { modeState.mode = ScannerMode.Default },
            label = { Text("Default") },
        )
        FilterChip(
            selected = modeState.mode == ScannerMode.Custom,
            onClick = { modeState.mode = ScannerMode.Custom },
            label = { Text("Custom") },
        )
        FilterChip(
            selected = modeState.mode == ScannerMode.Image,
            onClick = { modeState.mode = ScannerMode.Image },
            label = { Text("Image") },
        )
    }
}
