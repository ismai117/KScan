import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.ncgroup.kscan.App
import org.ncgroup.kscan.KScanWeb

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    KScanWeb.debugLogging = true

    ComposeViewport(document.getElementById("app")!!) {
        App()
    }
}
