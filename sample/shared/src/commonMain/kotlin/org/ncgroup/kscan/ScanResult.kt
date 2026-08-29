package org.ncgroup.kscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shows whatever the last scan produced, if anything. */
@Composable
fun ScanResult(
    scan: ScanState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (scan.hasResult) {
            Text(text = "Data: ${scan.data}")
            Text(text = "Format: ${scan.format}")
        }

        if (scan.error.isNotEmpty()) {
            Text(text = scan.error, color = Color.Red)
        }
    }
}
