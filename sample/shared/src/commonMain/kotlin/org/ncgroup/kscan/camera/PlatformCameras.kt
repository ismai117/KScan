package org.ncgroup.kscan.camera

import org.ncgroup.kscan.CameraDevice

// KScan lists cameras on web and desktop only; elsewhere the platform picks.
expect suspend fun platformCameras(): List<CameraDevice>
