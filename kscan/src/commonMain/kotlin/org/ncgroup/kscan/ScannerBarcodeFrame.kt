package org.ncgroup.kscan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A composable function that draws a frame for a barcode scanner.
 *
 * @param modifier The modifier to be applied to the frame.
 * @param frameColor The color of the frame.
 * @param frameWidth The width of the frame.
 * @param cornerLength The length of the corners of the frame.
 */
@Composable
internal fun ScannerBarcodeFrame(
    modifier: Modifier = Modifier,
    frameColor: Color = Color(0xFFF050F8),
    frameWidth: Float = 5f,
    cornerLength: Float = 24f,
) {
    Box(
        modifier = modifier.size(260.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val strokeWidth = frameWidth.dp.toPx()
            val radius = cornerLength.dp.toPx()

            drawArc(
                color = frameColor,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, 0f),
                size = Size(radius * 2, radius * 2),
                style = Stroke(strokeWidth),
            )

            drawArc(
                color = frameColor,
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(width - radius * 2, 0f),
                size = Size(radius * 2, radius * 2),
                style = Stroke(strokeWidth),
            )

            drawArc(
                color = frameColor,
                startAngle = 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(0f, height - radius * 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(strokeWidth),
            )

            drawArc(
                color = frameColor,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(width - radius * 2, height - radius * 2),
                size = Size(radius * 2, radius * 2),
                style = Stroke(strokeWidth),
            )
        }
    }
}
