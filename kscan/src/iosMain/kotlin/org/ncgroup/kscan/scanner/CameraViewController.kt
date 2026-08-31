package org.ncgroup.kscan.scanner

import kotlinx.cinterop.ExperimentalForeignApi
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.format.BarcodeFormatMapper
import org.ncgroup.kscan.format.isRequestedFormat
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoOrientation
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeLeft
import platform.AVFoundation.AVCaptureVideoOrientationLandscapeRight
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoOrientationPortraitUpsideDown
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.videoZoomFactor
import platform.Foundation.NSLog
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

internal class CameraViewController(
    private val device: AVCaptureDevice,
    private val codeTypes: List<BarcodeFormat>,
    private val onBarcodeSuccess: (List<Barcode>) -> Unit,
    private val onBarcodeFailed: (Exception) -> Unit,
    private val filter: (Barcode) -> Boolean,
    private val onMaxZoomRatioAvailable: (Float) -> Unit,
) : UIViewController(null, null),
    AVCaptureMetadataOutputObjectsDelegateProtocol {
    private lateinit var captureSession: AVCaptureSession
    private lateinit var previewLayer: AVCaptureVideoPreviewLayer
    private lateinit var videoInput: AVCaptureDeviceInput

    private val repeated = RepeatedDetection()

    // The session stops on a background queue while metadata arrives on the main
    // one, so a frame already queued can still be delivered after disposal.
    private var disposed = false

    private val maxZoomRatio: Float by lazy {
        device.activeFormat.videoMaxZoomFactor.toFloat().coerceAtMost(MAX_ZOOM_RATIO)
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.blackColor
        setupCamera()
        onMaxZoomRatioAvailable(maxZoomRatio)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupCamera() {
        captureSession = AVCaptureSession()

        videoInput = try {
            AVCaptureDeviceInput.deviceInputWithDevice(device, null) as AVCaptureDeviceInput
        } catch (e: Exception) {
            onBarcodeFailed(e)
            return
        }

        val metadataOutput = AVCaptureMetadataOutput()

        if (!captureSession.canAddInput(videoInput)) {
            onBarcodeFailed(Exception("Failed to add video input"))
            return
        }
        captureSession.addInput(videoInput)

        if (!captureSession.canAddOutput(metadataOutput)) {
            onBarcodeFailed(Exception("Failed to add metadata output"))
            return
        }
        captureSession.addOutput(metadataOutput)

        setupMetadataOutput(metadataOutput)
        setupPreviewLayer()
        // Starting is left to viewWillAppear, which also covers re-appearing.
    }

    private fun setupMetadataOutput(metadataOutput: AVCaptureMetadataOutput) {
        metadataOutput.setMetadataObjectsDelegate(this, dispatch_get_main_queue())

        // Assigning a type this device or OS release does not offer raises an
        // NSInvalidArgumentException, and which types exist is only knowable once
        // the output belongs to the session. Codabar, for one, arrived in iOS 15.4.
        val available = metadataOutput.availableMetadataObjectTypes
        val supportedTypes = BarcodeFormatMapper.toAvTypes(codeTypes).filter { it in available }

        if (supportedTypes.isEmpty()) {
            onBarcodeFailed(Exception("No supported barcode types selected"))
            return
        }

        metadataOutput.metadataObjectTypes = supportedTypes
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupPreviewLayer() {
        previewLayer = AVCaptureVideoPreviewLayer.layerWithSession(captureSession)
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        view.layer.addSublayer(previewLayer)
        updatePreviewOrientation()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        offMainQueue {
            if (::captureSession.isInitialized && !captureSession.isRunning()) {
                captureSession.startRunning()
            }
        }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        stopSession()
    }

    // Starting and stopping a session blocks, so it is kept off the main queue.
    private fun offMainQueue(block: () -> Unit) {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u), block)
    }

    private fun stopSession() = offMainQueue {
        if (::captureSession.isInitialized && captureSession.isRunning()) {
            captureSession.stopRunning()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        if (!::previewLayer.isInitialized) return

        previewLayer.frame = view.layer.bounds
        updatePreviewOrientation()
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        processBarcodes(didOutputMetadataObjects)
    }

    private fun processBarcodes(metadataObjects: List<*>) {
        if (disposed) return

        // Only type, value and descriptor are read, so converting these into
        // preview-layer coordinates would discard the result.
        metadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .forEach { detection -> report(detection) }
    }

    private fun report(detection: AVMetadataMachineReadableCodeObject) {
        val barcode = detection.toBarcode()

        if (!isRequestedFormat(barcode.format, codeTypes)) return
        // A barcode the OS gives no value for is still worth counting, so it falls
        // back to the type to tell one from another.
        if (!repeated.accept(barcode.data.ifEmpty { detection.type.toString() })) return
        if (!filter(barcode)) return

        onBarcodeSuccess(listOf(barcode))
        repeated.reset()
        stopSession()
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setZoom(ratio: Float): Float {
        var locked = false
        try {
            locked = device.lockForConfiguration(null)
            if (locked) {
                device.videoZoomFactor = ratio.coerceIn(1f, maxZoomRatio).toDouble()
            }
        } catch (e: Exception) {
            NSLog("Failed to update zoom: %@", e.message ?: "unknown")
        } finally {
            if (locked) device.unlockForConfiguration()
        }

        return device.videoZoomFactor.toFloat()
    }

    private fun updatePreviewOrientation() {
        if (!::previewLayer.isInitialized) return

        val connection = previewLayer.connection ?: return

        val uiOrientation: UIInterfaceOrientation = UIApplication.sharedApplication().statusBarOrientation

        val videoOrientation: AVCaptureVideoOrientation =
            when (uiOrientation) {
                UIInterfaceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeLeft
                UIInterfaceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeRight
                UIInterfaceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
                else -> AVCaptureVideoOrientationPortrait
            }

        connection.videoOrientation = videoOrientation
    }

    fun dispose() {
        disposed = true

        offMainQueue {
            runCatching {
                if (::captureSession.isInitialized) {
                    if (captureSession.isRunning()) captureSession.stopRunning()

                    // Removed so the session does not retain them.
                    captureSession.outputs.filterIsInstance<AVCaptureOutput>()
                        .forEach { runCatching { captureSession.removeOutput(it) } }
                    captureSession.inputs.filterIsInstance<AVCaptureDeviceInput>()
                        .forEach { runCatching { captureSession.removeInput(it) } }
                }
            }
        }

        runCatching {
            if (::previewLayer.isInitialized) previewLayer.removeFromSuperlayer()
        }
        repeated.reset()
    }
}
