package org.ncgroup.kscan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberImagePicker(onImage: (ByteArray) -> Unit): () -> Unit {
    val currentOnImage by rememberUpdatedState(onImage)
    val scope = rememberCoroutineScope()

    return remember {
        {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    val dialog = FileDialog(null as Frame?, "Pick an image", FileDialog.LOAD)
                    dialog.setFilenameFilter { _, name ->
                        name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
                    }
                    dialog.isVisible = true

                    dialog.file?.let { File(dialog.directory, it).readBytes() }
                }

                bytes?.let(currentOnImage)
            }
            Unit
        }
    }
}

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp", "heic")
