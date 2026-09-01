package org.ncgroup.kscan.scanner

import org.bytedeco.javacv.OpenCVFrameGrabber
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

// How far past the requested index to look before giving up.
internal const val LAST_CAMERA_INDEX = 4

// A camera that will open does so in well under a second, while an index nothing
// answers on blocks for ten, and one macOS will not hand over for nearly two
// minutes. start() cannot be interrupted, so it runs on a thread of its own and
// is abandoned when it takes too long: without that, opening the camera or
// listing what is attached costs minutes rather than seconds.
private const val OPEN_TIMEOUT_MS = 3000L

private val openers = Executors.newCachedThreadPool { runnable ->
    Thread(runnable, "kscan-camera-open").apply { isDaemon = true }
}

internal fun OpenCVFrameGrabber.releaseQuietly() {
    runCatching { stop() }
    runCatching { release() }
}

/** A started grabber for [index], or `null` when it does not open in time. */
internal fun startGrabber(index: Int): OpenCVFrameGrabber? {
    val grabber = OpenCVFrameGrabber(index).apply {
        imageWidth = 1920
        imageHeight = 1080
        setVideoOption("focus_auto", "1")
    }

    val started = CompletableFuture.supplyAsync(
        { runCatching { grabber.start() }.isSuccess },
        openers,
    )

    return try {
        if (started.get(OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            grabber
        } else {
            grabber.releaseQuietly()
            null
        }
    } catch (e: TimeoutException) {
        // Whatever it is waiting on outlives this call, so it is released there.
        started.whenComplete { _, _ -> grabber.releaseQuietly() }
        null
    }
}

// Which index names which camera is the platform's choice and shifts as devices
// are plugged and unplugged, so the index that worked last run can be busy or
// missing this one.
internal fun openCamera(cameraId: String?): OpenCVFrameGrabber {
    val requested = cameraId?.toIntOrNull()
    val candidates = listOfNotNull(requested) + (0..LAST_CAMERA_INDEX).filter { it != requested }

    for (index in candidates) {
        startGrabber(index)?.let { return it }
    }

    throw IllegalStateException("No camera could be opened")
}
