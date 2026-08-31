package org.ncgroup.kscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.OpenCVFrameGrabber
import org.ncgroup.kscan.scanner.LAST_CAMERA_INDEX

/**
 * Lists the cameras this machine will open, for [ScannerView]'s `cameraId`.
 *
 * OpenCV cannot be asked what cameras exist, so each index is opened and closed
 * to find out, and the labels are positions rather than names. Opening a camera
 * is slow and lights its indicator, so call this once and hold the result rather
 * than on every recomposition, and not while a scan is running: a camera already
 * in use by the scanner, or by another application, does not appear here.
 */
public suspend fun availableCameras(): List<CameraDevice> = withContext(Dispatchers.IO) {
    (0..LAST_CAMERA_INDEX).mapNotNull { index ->
        val grabber = OpenCVFrameGrabber(index)

        try {
            grabber.start()
            CameraDevice(id = index.toString(), label = "Camera $index")
        } catch (e: Exception) {
            null
        } finally {
            runCatching { grabber.stop() }
            runCatching { grabber.release() }
        }
    }
}
