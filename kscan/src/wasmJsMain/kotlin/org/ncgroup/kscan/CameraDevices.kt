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
 * Lists the cameras available to the browser, for [KScanWeb.cameraDeviceId].
 *
 * Labels are blank until the user has granted camera access, so call this again
 * after a scan has started to show a meaningful picker.
 */
public suspend fun availableCameras(): List<CameraDevice> {
    val devices = enumerateCameras().await()

    return buildList {
        for (index in 0 until devices.length) {
            devices[index]?.let { add(CameraDevice(it.id.toString(), it.label.toString())) }
        }
    }
}
