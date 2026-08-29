package org.ncgroup.kscan

import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import kotlin.test.Test

class CameraViewControllerDisposeTest {
    private fun makeController(device: AVCaptureDevice): CameraViewController = CameraViewController(
        device = device,
        codeTypes = emptyList(),
        onBarcodeSuccess = { /* no-op */ },
        onBarcodeFailed = { /* no-op */ },
        filter = { true },
        onMaxZoomRatioAvailable = { /* no-op */ },
    )

    @Test
    fun dispose_before_viewDidLoad_is_safe() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val controller = makeController(device)
        // Should not throw even if session/previewLayer were never initialized
        controller.dispose()
    }

    @Test
    fun dispose_twice_is_idempotent() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val controller = makeController(device)
        controller.dispose()
        controller.dispose()
    }
}
