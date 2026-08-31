package org.ncgroup.kscan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.ncgroup.kscan.formats.FormatListScreen
import org.ncgroup.kscan.image.ImageScannerScreen
import org.ncgroup.kscan.scanner.ScannerScreen
import org.ncgroup.kscan.scanner.ScannerState
import org.ncgroup.kscan.settings.SettingsScreen

@Composable
fun App() {
    val state = remember { ScannerState() }
    val backStack = rememberNavBackStack(navConfig, Route.Formats)
    val back: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = back,
        entryProvider = entryProvider {
            entry<Route.Formats> {
                FormatListScreen(
                    onFormat = { backStack.add(Route.Scanner(it)) },
                    onImage = { backStack.add(Route.ImageScan) },
                )
            }

            entry<Route.Scanner> { route ->
                ScannerScreen(
                    format = route.format,
                    state = state,
                    onBack = back,
                    onSettings = { backStack.add(Route.Settings) },
                )
            }

            entry<Route.Settings> {
                SettingsScreen(state = state, onBack = back)
            }

            entry<Route.ImageScan> {
                ImageScannerScreen(onBack = back)
            }
        },
    )
}
