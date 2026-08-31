package org.ncgroup.kscan.formats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ncgroup.kscan.BarcodeFormat

private data class FormatEntry(
    val format: BarcodeFormat,
    val label: String,
    val note: String? = null,
)

private val twoDimensional = listOf(
    FormatEntry(BarcodeFormat.FORMAT_QR_CODE, "QR Code"),
    FormatEntry(BarcodeFormat.FORMAT_AZTEC, "Aztec"),
    FormatEntry(BarcodeFormat.FORMAT_DATA_MATRIX, "Data Matrix"),
    FormatEntry(BarcodeFormat.FORMAT_PDF417, "PDF417"),
)

private val oneDimensional = listOf(
    FormatEntry(BarcodeFormat.FORMAT_CODE_128, "Code 128"),
    FormatEntry(BarcodeFormat.FORMAT_CODE_39, "Code 39"),
    FormatEntry(BarcodeFormat.FORMAT_CODE_93, "Code 93"),
    FormatEntry(BarcodeFormat.FORMAT_CODABAR, "Codabar", "Needs iOS 15.4"),
    FormatEntry(BarcodeFormat.FORMAT_EAN_13, "EAN-13"),
    FormatEntry(BarcodeFormat.FORMAT_EAN_8, "EAN-8"),
    FormatEntry(BarcodeFormat.FORMAT_ITF, "ITF"),
    FormatEntry(BarcodeFormat.FORMAT_UPC_A, "UPC-A", "Scans as EAN-13 on iOS"),
    FormatEntry(BarcodeFormat.FORMAT_UPC_E, "UPC-E"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatListScreen(
    onFormat: (BarcodeFormat) -> Unit,
    onImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = "KScan") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item {
                ActionItem(
                    label = "All formats",
                    description = "Scan for every supported format",
                    icon = Icons.Filled.QrCodeScanner,
                    onClick = { onFormat(BarcodeFormat.FORMAT_ALL_FORMATS) },
                )
            }

            item {
                ActionItem(
                    label = "Scan from image",
                    description = "Pick a picture instead of using the camera",
                    icon = Icons.Filled.Image,
                    onClick = onImage,
                )

                HorizontalDivider()
            }

            sectionHeader("2D Barcodes")
            formatItems(twoDimensional, onFormat)

            sectionHeader("1D Barcodes")
            formatItems(oneDimensional, onFormat)
        }
    }
}

@Composable
private fun ActionItem(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = label) },
        supportingContent = { Text(text = description) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = { Chevron() },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline,
    )
}

private fun LazyListScope.sectionHeader(title: String) {
    item {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 8.dp),
        )
    }
}

private fun LazyListScope.formatItems(
    entries: List<FormatEntry>,
    onFormat: (BarcodeFormat) -> Unit,
) {
    items(entries) { entry ->
        ListItem(
            headlineContent = { Text(text = entry.label) },
            supportingContent = entry.note?.let { note -> { Text(text = note) } },
            trailingContent = { Chevron() },
            modifier = Modifier.clickable { onFormat(entry.format) },
        )
    }
}
