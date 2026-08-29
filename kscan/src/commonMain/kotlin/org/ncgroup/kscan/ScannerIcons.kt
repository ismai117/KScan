package org.ncgroup.kscan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The handful of Material icons the scanner UI draws.
 *
 * These are declared here rather than pulled from `material-icons-extended`,
 * which ships the entire Material set — over 20 MB — for the five icons used
 * below. Path data is taken verbatim from Google's material-design-icons
 * repository, so the icons render identically to the originals.
 */
internal object ScannerIcons {
    val Add: ImageVector by lazy {
        materialIcon("Add", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    }

    val Remove: ImageVector by lazy {
        materialIcon("Remove", "M19 13H5v-2h14v2z")
    }

    val Cancel: ImageVector by lazy {
        materialIcon(
            "Cancel",
            "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 " +
                "13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 " +
                "10.59 15.59 7 17 8.41 13.41 12 17 15.59z",
        )
    }

    val FlashOn: ImageVector by lazy {
        materialIcon("FlashOn", "M7 2v11h3v9l7-12h-4l4-8z")
    }

    val FlashOff: ImageVector by lazy {
        materialIcon(
            "FlashOff",
            "M3.27 3L2 4.27l5 5V13h3v9l3.58-6.14L17.73 20 19 18.73 3.27 3zM17 " +
                "10h-4l4-8H7v2.18l8.46 8.46L17 10z",
        )
    }
}

/** Builds a 24dp icon from SVG [pathData], matching the Material icon geometry. */
private fun materialIcon(name: String, pathData: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(pathData),
    fill = SolidColor(Color.Black),
).build()
