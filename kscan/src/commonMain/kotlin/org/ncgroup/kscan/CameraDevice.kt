package org.ncgroup.kscan

/**
 * A camera [ScannerView] can be pointed at, from `availableCameras()`.
 *
 * @property id Pass to [ScannerView]'s `cameraId`.
 * @property label What to show in a picker. Blank where the platform gives no name.
 */
public data class CameraDevice(
    val id: String,
    val label: String,
)
