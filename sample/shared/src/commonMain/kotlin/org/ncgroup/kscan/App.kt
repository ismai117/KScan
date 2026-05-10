package org.ncgroup.kscan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class ScannerMode {
    Default,
    Custom,
    Image,
}

class ScannerModeState(
    initial: ScannerMode = ScannerMode.Default
) {
    var mode by mutableStateOf(initial)
}

val LocalScannerModeState = compositionLocalOf { ScannerModeState() }

@Composable
fun App() {
    val scannerModeState = remember { ScannerModeState() }

    CompositionLocalProvider(LocalScannerModeState provides scannerModeState) {
        when (scannerModeState.mode) {
            ScannerMode.Default -> DefaultUI()
            ScannerMode.Custom -> CustomUI()
            ScannerMode.Image -> ImageScannerUI()
        }
    }
}
