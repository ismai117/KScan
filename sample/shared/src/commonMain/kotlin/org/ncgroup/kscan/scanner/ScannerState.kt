package org.ncgroup.kscan.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.ncgroup.kscan.Barcode

class ScannerState {
    var autoZoom by mutableStateOf(true)
    var filterEnabled by mutableStateOf(false)
    var filterPrefix by mutableStateOf("")

    var scanned by mutableStateOf<Barcode?>(null)

    fun accepts(barcode: Barcode): Boolean = !filterEnabled || barcode.data.startsWith(filterPrefix)
}
