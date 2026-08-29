package org.ncgroup.kscan

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The outcome of the last scan, however it was started. */
@Stable
class ScanState {
    var data by mutableStateOf("")
        private set

    var format by mutableStateOf("")
        private set

    var error by mutableStateOf("")
        private set

    val hasResult: Boolean get() = data.isNotEmpty()

    fun accept(result: BarcodeResult) {
        when (result) {
            is BarcodeResult.OnSuccess -> {
                data = result.barcode.data
                format = result.barcode.format
                error = ""
            }

            is BarcodeResult.OnFailed -> {
                error = "Error: ${result.exception.message}"
            }

            BarcodeResult.OnCanceled -> Unit
        }
    }

    fun clear() {
        data = ""
        format = ""
        error = ""
    }
}
