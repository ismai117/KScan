import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.ncgroup.kscan.App
import org.ncgroup.kscan.KScanWeb

private val APP_WIDTH = 1200.dp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    KScanWeb.debugLogging = true

    ComposeViewport(document.getElementById("app")!!) {
        CenteredApp()
    }
}

@Composable
private fun CenteredApp() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = APP_WIDTH).fillMaxHeight()) {
            App()
        }
    }
}
