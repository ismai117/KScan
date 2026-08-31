package org.ncgroup.kscan.camera

import org.ncgroup.kscan.CameraDevice

// KScan lists cameras on web and desktop only; elsewhere the platform picks.
expect suspend fun platformCameras(): List<CameraDevice>

// False where KScan cannot list at all, so the sample can say so rather than
// showing the same empty space it would for a machine with no camera.
expect val supportsCameraListing: Boolean
