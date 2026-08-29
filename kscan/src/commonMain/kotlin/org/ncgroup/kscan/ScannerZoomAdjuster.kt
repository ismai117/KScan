package org.ncgroup.kscan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A Composable function that provides UI controls for adjusting the zoom level of a scanner.
 * It displays two buttons, one for zooming in and one for zooming out.
 *
 * @param modifier Optional [Modifier] for this composable.
 * @param zoomRatio The current zoom ratio of the scanner.
 * @param zoomRatioOnChange A callback function that is invoked when the zoom ratio changes.
 * It takes the new zoom ratio as a Float parameter.
 * @param maxZoomRatio The maximum zoom ratio allowed for the scanner.
 * @param zoomStep The increment or decrement value for each zoom adjustment. Defaults to 0.5f.
 * @param containerColor The background color of the zoom adjuster card. Defaults to Color(0xFF291544).
 * @param contentColor The color of the icons and text within the zoom adjuster card. Defaults to Color.White.
 */
@Composable
internal fun ScannerZoomAdjuster(
    zoomRatio: Float,
    zoomRatioOnChange: (Float) -> Unit,
    maxZoomRatio: Float,
    modifier: Modifier = Modifier,
    zoomStep: Float = 0.5f,
    containerColor: Color = Color(0xFF291544),
    contentColor: Color = Color.White,
) {
    ElevatedCard(
        modifier = modifier,
        colors =
        CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    decreaseZoom(zoomRatio, zoomStep, zoomRatioOnChange)
                },
            ) {
                Icon(
                    imageVector = ScannerIcons.Remove,
                    contentDescription = "Zoom Out",
                    tint = contentColor,
                )
            }

            Spacer(modifier = Modifier.width(80.dp))

            IconButton(
                onClick = {
                    increaseZoom(zoomRatio, zoomStep, maxZoomRatio, zoomRatioOnChange)
                },
            ) {
                Icon(
                    imageVector = ScannerIcons.Add,
                    contentDescription = "Zoom In",
                    tint = contentColor,
                )
            }
        }
    }
}

private fun decreaseZoom(
    currentZoom: Float,
    zoomStep: Float,
    zoomRatioOnChange: (Float) -> Unit,
) {
    val newZoom = (currentZoom - zoomStep).coerceAtLeast(1f)
    zoomRatioOnChange(newZoom)
}

private fun increaseZoom(
    currentZoom: Float,
    zoomStep: Float,
    maxZoom: Float,
    zoomRatioOnChange: (Float) -> Unit,
) {
    val newZoom = (currentZoom + zoomStep).coerceAtMost(maxZoom)
    zoomRatioOnChange(newZoom)
}
