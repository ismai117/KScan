package org.ncgroup.kscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.ncgroup.kscan.scanner.LAST_CAMERA_INDEX
import org.ncgroup.kscan.scanner.releaseQuietly
import org.ncgroup.kscan.scanner.startGrabber

/**
 * Lists the cameras this machine will open, for [ScannerView]'s `cameraId`.
 *
 * OpenCV cannot be asked what cameras exist, so every index is opened at once to
 * find out and the labels are positions rather than names. An index that does not
 * answer within a few seconds is taken to be nothing, so a camera the scanner
 * still holds, or that another application has, does not appear. It costs a few
 * seconds and lights the indicator of every camera it finds, so call it when a
 * picker is shown rather than on every recomposition.
 */
public suspend fun availableCameras(): List<CameraDevice> = withContext(Dispatchers.IO) {
    (0..LAST_CAMERA_INDEX)
        .map { index ->
            async {
                startGrabber(index)?.let { grabber ->
                    grabber.releaseQuietly()
                    CameraDevice(id = index.toString(), label = "Camera $index")
                }
            }
        }
        .awaitAll()
        .filterNotNull()
}
