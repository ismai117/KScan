@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ncgroup.kscan

import kotlinx.coroutines.await
import kotlin.js.Promise
import kotlin.js.get

/** A camera the browser is willing to open. */
public data class CameraDevice(
    val id: String,
    val label: String,
)

private external interface JsCameraDevice : JsAny {
    val id: JsString
    val label: JsString
}

private fun enumerateCameras(): Promise<JsArray<JsCameraDevice>> = js(
    """(async () => {
            const devices = await navigator.mediaDevices.enumerateDevices();
            return devices
                .filter((device) => device.kind === 'videoinput')
                .map((device) => ({ id: device.deviceId, label: device.label }));
        })()""",
)

/**
 * Lists the cameras available to the browser.
 *
 * Pass a device's [CameraDevice.id] to [KScanWeb.cameraDeviceId] to scan with it.
 *
 * Labels are only filled in once the user has granted camera access, so calling
 * this before the first scan returns entries with an empty [CameraDevice.label].
 * Call it again after a scan has started to show a meaningful picker.
 *
 * On macOS a nearby iPhone appears here through Continuity Camera, named as the
 * phone.
 */
public suspend fun availableCameras(): List<CameraDevice> {
    val devices = enumerateCameras().await()

    return buildList {
        for (index in 0 until devices.length) {
            devices[index]?.let { add(CameraDevice(it.id.toString(), it.label.toString())) }
        }
    }
}
