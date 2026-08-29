package org.ncgroup.kscan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The scanner with its own UI switched off and torch and zoom driven by hand. */
@Composable
fun CustomUI(modifier: Modifier = Modifier) {
    val scannerController = remember { ScannerController() }

    ScannerScreen(
        modifier = modifier,
        scannerUiOptions = null,
        scannerController = scannerController,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp).align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { scannerController.setTorch(!scannerController.torchEnabled) }) {
                Text(text = "Torch ${if (scannerController.torchEnabled) "On" else "Off"}")
            }

            Text(text = "Zoom: ${scannerController.zoomRatio}")

            Slider(
                value = scannerController.zoomRatio,
                onValueChange = scannerController::setZoom,
                valueRange = 1f..scannerController.maxZoomRatio,
                steps = maxOf(0, (scannerController.maxZoomRatio - 1f).toInt() - 1),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
