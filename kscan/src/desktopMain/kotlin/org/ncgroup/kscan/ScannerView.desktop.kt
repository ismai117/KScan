package org.ncgroup.kscan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ScannerView(
    modifier: Modifier,
    codeTypes: List<BarcodeFormat>,
    colors: ScannerColors,
    showUi: Boolean,
    result: (BarcodeResult) -> Unit,
) {}
