package org.ncgroup.kscan

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A composable function that displays a scanner header with a title, cancel button, and torch toggle button.
 *
 * @param modifier The modifier to be applied to the scanner header.
 * @param onCancel A callback function that is invoked when the cancel button is clicked.
 * @param showTorch A boolean value that determines whether to show the torch button or not.
 * @param torchEnabled A boolean value that indicates whether the torch is enabled.
 * @param onTorchChange A callback function that is invoked when the torch toggle button is clicked.
 * @param containerColor The color of the container.
 * @param navigationIconColor The color of the navigation icon.
 * @param titleColor The color of the title.
 * @param actionIconColor The color of the action icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerHeader(
    onCancel: () -> Unit,
    showTorch: Boolean,
    torchEnabled: Boolean,
    onTorchChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF291544),
    navigationIconColor: Color = Color.White,
    titleColor: Color = Color.White,
    actionIconColor: Color = Color.White,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    onCancel()
                },
            ) {
                Icon(
                    imageVector = ScannerIcons.Cancel,
                    contentDescription = null,
                )
            }
        },
        actions = {
            if (showTorch) {
                IconButton(
                    onClick = {
                        if (torchEnabled) {
                            onTorchChange(false)
                        } else {
                            onTorchChange(true)
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (torchEnabled) ScannerIcons.FlashOn else ScannerIcons.FlashOff,
                        contentDescription = null,
                    )
                }
            }
        },
        colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            navigationIconContentColor = navigationIconColor,
            titleContentColor = titleColor,
            actionIconContentColor = actionIconColor,
        ),
        modifier = modifier,
    )
}
