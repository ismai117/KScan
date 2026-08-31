package org.ncgroup.kscan.scanner

import org.bytedeco.javacv.OpenCVFrameGrabber

/** How far past the requested index to look before giving up. */
private const val LAST_CAMERA_INDEX = 4

/**
 * Starts the camera at [preferred], or the first other one that will open.
 *
 * Which index names which camera is the platform's choice and shifts as devices
 * are plugged and unplugged, so the index that worked last run can be missing or
 * busy this one. Falling through to the next index means unplugging a webcam is
 * enough to scan with whatever is left.
 */
internal fun openCamera(preferred: Int): OpenCVFrameGrabber {
    val candidates = listOf(preferred) + (0..LAST_CAMERA_INDEX).filter { it != preferred }
    var failure: Exception? = null

    for (index in candidates) {
        val grabber = OpenCVFrameGrabber(index).apply {
            imageWidth = 1920
            imageHeight = 1080
            setVideoOption("focus_auto", "1")
        }

        try {
            grabber.start()
            return grabber
        } catch (e: Exception) {
            failure = e
            // A index that will not start may still hold resources.
            runCatching { grabber.release() }
        }
    }

    throw failure ?: IllegalStateException("No camera could be opened")
}
