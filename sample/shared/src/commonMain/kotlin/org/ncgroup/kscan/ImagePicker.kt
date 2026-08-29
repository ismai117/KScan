package org.ncgroup.kscan

import androidx.compose.runtime.Composable

/**
 * Remembers a way to pick an image from the device.
 *
 * The returned lambda opens the platform's picker; [onImage] receives the bytes
 * of whatever was chosen, and is not called if the user cancels.
 */
@Composable
expect fun rememberImagePicker(onImage: (ByteArray) -> Unit): () -> Unit
