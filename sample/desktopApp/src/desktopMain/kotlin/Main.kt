import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.ncgroup.kscan.App
import org.ncgroup.kscan.KScanDesktop

fun main() = application {
    // Index 1 is often a nearby iPhone on macOS, offered through Continuity Camera.
    KScanDesktop.cameraIndex = System.getProperty("kscan.camera")?.toIntOrNull() ?: 0

    val windowState = rememberWindowState(
        size = DpSize(width = 400.dp, height = 800.dp),
        position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center),
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        alwaysOnTop = true,
        title = "Barcodescanner",
    ) {
        App()
    }
}
