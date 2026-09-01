package org.ncgroup.kscan.camera

import org.ncgroup.kscan.CameraDevice
import org.ncgroup.kscan.availableCameras

actual suspend fun platformCameras(): List<CameraDevice> = availableCameras()

actual val supportsCameraListing: Boolean = true
