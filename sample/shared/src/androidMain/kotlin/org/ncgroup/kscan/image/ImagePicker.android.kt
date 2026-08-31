package org.ncgroup.kscan.image

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(onImage: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnImage by rememberUpdatedState(onImage)

    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                currentOnImage(stream.readBytes())
            }
        }
    }

    return remember(launcher) {
        {
            launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
    }
}
