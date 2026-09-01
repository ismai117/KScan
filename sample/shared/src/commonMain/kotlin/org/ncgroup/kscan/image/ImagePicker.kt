package org.ncgroup.kscan.image

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(onImage: (ByteArray) -> Unit): () -> Unit
