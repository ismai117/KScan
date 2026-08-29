@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ncgroup.kscan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

/** Opens a file picker and resolves with the file's contents, base64 encoded. */
private fun pickImageAsBase64(): Promise<JsString> = js(
    """(new Promise((resolve, reject) => {
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = 'image/*';
            input.onchange = () => {
                const file = input.files && input.files[0];
                if (!file) {
                    resolve('');
                    return;
                }
                const reader = new FileReader();
                reader.onload = () => {
                    const dataUrl = reader.result;
                    resolve(dataUrl.substring(dataUrl.indexOf(',') + 1));
                };
                reader.onerror = () => reject(reader.error || new Error('Could not read file'));
                reader.readAsDataURL(file);
            };
            input.click();
        }))""",
)

@OptIn(ExperimentalEncodingApi::class)
@Composable
actual fun rememberImagePicker(onImage: (ByteArray) -> Unit): () -> Unit {
    val currentOnImage by rememberUpdatedState(onImage)

    return remember {
        {
            pickImageAsBase64().then<JsAny?> { encoded ->
                encoded.toString().takeIf { it.isNotEmpty() }?.let {
                    currentOnImage(Base64.decode(it))
                }
                null
            }
            Unit
        }
    }
}
