package org.ncgroup.kscan

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class ScannerColors(
    val headerContainerColor: Color = Color(0xFF291544),
    val headerNavigationIconColor: Color = Color.White,
    val headerTitleColor: Color = Color.White,
    val headerActionIconColor: Color = Color.White,
    val zoomControllerContainerColor: Color = Color(0xFF291544),
    val zoomControllerContentColor: Color = Color.White,
    val barcodeFrameColor: Color = Color(0xFFF050F8),
)
