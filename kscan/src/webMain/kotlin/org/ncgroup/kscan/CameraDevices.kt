@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ncgroup.kscan

import kotlinx.coroutines.await
import kotlin.js.Promise
import kotlin.js.get

private external interface JsCameraDevice : JsAny {
    val id: JsString
    val label: JsString
}

private fun enumerateCameras(): Promise<JsArray<JsCameraDevice>> = js(
    """navigator.mediaDevices.enumerateDevices().then((devices) => devices
            .filter((device) => device.kind === 'videoinput')
            .map(function (device) {
                return { id: device.deviceId, label: device.label };
            }))""",
)

/**
 * Lists the cameras the browser will open, for [ScannerView]'s `cameraId`.
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
