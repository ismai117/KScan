package org.ncgroup.kscan

import androidx.compose.runtime.Immutable

@Immutable
public data class ScannerUiOptions(
    val headerTitle: String = "Scan Code",
    val showZoom: Boolean = true,
    val showTorch: Boolean = true,
)
