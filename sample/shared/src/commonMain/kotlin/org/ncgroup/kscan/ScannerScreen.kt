package org.ncgroup.kscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared shell for the live-camera modes: shows the last result and a button to
 * scan again, and swaps in [ScannerView] while scanning.
 *
 * [scannerUiOptions] is passed through, so `null` hands the scanner's own chrome
 * over to [overlay], which is drawn above the preview.
 */
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    scannerUiOptions: ScannerUiOptions? = ScannerUiOptions(),
    scannerController: ScannerController? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    var showScanner by remember { mutableStateOf(false) }
    val scan = remember { ScanState() }

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
                ScanResult(scan)

                Button(onClick = { showScanner = true }) {
                    Text(text = "Scan Barcode")
                }
            }

            if (showScanner) {
                ScannerView(
                    codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
                    scannerUiOptions = scannerUiOptions,
                    scannerController = scannerController,
                ) { result ->
                    scan.accept(result)
                    showScanner = false
                }

                overlay()
            }
        }
    }
}
