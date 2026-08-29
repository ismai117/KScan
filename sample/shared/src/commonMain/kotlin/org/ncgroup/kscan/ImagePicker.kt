package org.ncgroup.kscan

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(onImage: (ByteArray) -> Unit): () -> Unit
