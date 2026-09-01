@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ncgroup.kscan.scanner

import org.w3c.dom.HTMLVideoElement
import kotlin.js.Promise

// Zoom is optional in the MediaStream spec, so it is queried once the stream is
// live rather than assumed.
internal external interface CameraCapabilities : JsAny {
    val maxZoomRatio: Double
}

// Starts hidden because Compose sizes the container a frame after the element is
// inserted, until when the 100% below resolves against nothing and leaves the
// video at its 300x150 default. The first frame arrives long after that.
internal fun createVideoElement(): HTMLVideoElement = js(
    """(() => {
            const video = document.createElement('video');
            video.autoplay = true;
            video.muted = true;
            video.playsInline = true;
            // Safari on iOS only honours the attribute form.
            video.setAttribute('playsinline', '');
            video.setAttribute('muted', '');
            video.style.display = 'block';
            video.style.width = '100%';
            video.style.height = '100%';
            video.style.objectFit = 'cover';
            // Hidden rather than absent, so it still takes up its layout box.
            video.style.visibility = 'hidden';
            video.addEventListener('loadeddata', () => {
                video.style.visibility = 'visible';
            });
            return video;
        })()""",
)

// Falls back to a bare video request because some devices and browsers reject a
// constraint set outright rather than negotiating down, even when all of it is ideal.
internal fun startCamera(
    video: HTMLVideoElement,
    deviceId: String,
    debug: Boolean,
): Promise<JsAny?> = js(
    """(() => {
            // Restarting attaches a second stream, and the tracks behind the first
            // go on holding the camera unless they are stopped here.
            if (video.srcObject) {
                video.srcObject.getTracks().forEach((track) => track.stop());
                video.srcObject = null;
            }

            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                return Promise.reject(new Error(
                    'Camera access requires a secure context (https or localhost)'
                ));
            }

            // Without a resolution the browser hands back 640x480, which is too
            // coarse to resolve most barcodes. A requested device is asked for
            // exactly; naming one and a facing mode together can conflict.
            const constraints = {
                width: { ideal: 1920 },
                height: { ideal: 1080 },
                frameRate: { ideal: 30 },
            };
            if (deviceId) {
                constraints.deviceId = { exact: deviceId };
            } else {
                constraints.facingMode = { ideal: 'environment' };
            }
            const preferred = { video: constraints, audio: false };

            const reportFailure = (second) => navigator.mediaDevices
                .enumerateDevices()
                .then(
                    (devices) => devices.filter((d) => d.kind === 'videoinput').length,
                    // enumerateDevices can itself be blocked.
                    () => 'unknown'
                )
                .then((cameras) => {
                    throw new Error(
                        'Could not start the camera (' + second.name + '). ' +
                        'Video inputs visible to this browser: ' + cameras + '. ' +
                        'Check that no other tab or app is using the camera and ' +
                        'that camera access is allowed for this site.'
                    );
                });

            return navigator.mediaDevices
                .getUserMedia(preferred)
                .catch((first) => {
                    if (debug) {
                        console.info(
                            '[KScan] preferred constraints rejected (' + first.name +
                            '), retrying with defaults'
                        );
                    }
                    return navigator.mediaDevices
                        .getUserMedia({ video: true, audio: false })
                        .catch(reportFailure);
                })
                .then((stream) => {
                    video.srcObject = stream;
                    return video.play().then(() => stream.getVideoTracks()[0]);
                })
                .then((track) => track
                    // Ignored where unsupported; matters on phones with autofocus.
                    .applyConstraints({ advanced: [{ focusMode: 'continuous' }] })
                    // Focus control is optional.
                    .catch(() => {})
                    .then(() => {
                        const settings = track.getSettings ? track.getSettings() : {};
                        if (debug) console.info(
                            '[KScan] camera ' + settings.width + 'x' + settings.height +
                            ' @' + settings.frameRate + 'fps'
                        );
                    }));
        })()""",
)

// Leaves the stream attached, so the element goes on painting its last frame.
internal fun freezeCamera(video: HTMLVideoElement): Unit = js(
    """(() => {
            const stream = video.srcObject;
            if (stream) {
                stream.getTracks().forEach((track) => track.stop());
            }
        })()""",
)

internal fun stopCamera(video: HTMLVideoElement): Unit = js(
    """(() => {
            const stream = video.srcObject;
            if (stream) {
                stream.getTracks().forEach((track) => track.stop());
                video.srcObject = null;
            }
        })()""",
)

internal fun cameraCapabilities(video: HTMLVideoElement): CameraCapabilities = js(
    """(() => {
            const stream = video.srcObject;
            const track = stream ? stream.getVideoTracks()[0] : null;
            const capabilities =
                track && track.getCapabilities ? track.getCapabilities() : {};
            const zoom = capabilities.zoom;
            return {
                maxZoomRatio: zoom && zoom.min > 0 ? zoom.max / zoom.min : 1.0,
            };
        })()""",
)

// Answers whether the track took it: applying torch to a camera that has none
// rejects, and the caller asked for a torch, not for a failed scan.
internal fun applyTorch(video: HTMLVideoElement, enabled: Boolean): Promise<JsBoolean> = js(
    """(() => {
            const stream = video.srcObject;
            const track = stream ? stream.getVideoTracks()[0] : null;
            if (!track || !track.getCapabilities) return Promise.resolve(false);
            if (track.getCapabilities().torch !== true) return Promise.resolve(false);

            return track
                .applyConstraints({ advanced: [{ torch: enabled }] })
                .then(() => true);
        })()""",
)

// ratio is a multiple of the track's minimum zoom, matching the 1f..maxZoomRatio
// range ScannerController exposes.
internal fun applyZoom(video: HTMLVideoElement, ratio: Double): Promise<JsAny?> = js(
    """(() => {
            const stream = video.srcObject;
            const track = stream ? stream.getVideoTracks()[0] : null;
            if (!track || !track.getCapabilities) return Promise.resolve();
            const zoom = track.getCapabilities().zoom;
            if (!zoom) return Promise.resolve();
            const target = Math.min(Math.max(zoom.min * ratio, zoom.min), zoom.max);
            return track.applyConstraints({ advanced: [{ zoom: target }] });
        })()""",
)

internal fun isVideoReady(video: HTMLVideoElement): Boolean = js("video.readyState >= 2 && video.videoWidth > 0")
