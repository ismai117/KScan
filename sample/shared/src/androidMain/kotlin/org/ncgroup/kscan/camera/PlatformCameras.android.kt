package org.ncgroup.kscan.camera

import org.ncgroup.kscan.CameraDevice

actual suspend fun platformCameras(): List<CameraDevice> = emptyList()

actual val supportsCameraListing: Boolean = false
