package org.ncgroup.kscan

import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.AVFoundation.AVMetadataObjectType
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

/**
 * UIViewController that manages camera preview and barcode scanning using AVFoundation.
 *
 * Features duplicate filtering (barcode must be detected twice) and zoom control.
 */
class CameraViewController(
    private val device: AVCaptureDevice,
    private val codeTypes: List<BarcodeFormat>,
    private val onBarcodeSuccess: (List<Barcode>) -> Unit,
    private val onBarcodeFailed: (Exception) -> Unit,
    private val onBarcodeCanceled: () -> Unit,
    private val filter: (Barcode) -> Boolean,
    private val onMaxZoomRatioAvailable: (Float) -> Unit,
) : UIViewController(null, null), AVCaptureMetadataOutputObjectsDelegateProtocol {
    private lateinit var captureSession: AVCaptureSession
    private lateinit var previewLayer: AVCaptureVideoPreviewLayer
    private lateinit var videoInput: AVCaptureDeviceInput

    private val barcodesDetected = mutableMapOf<String, Int>()

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.blackColor
        setupCamera()
        onMaxZoomRatioAvailable(device.activeFormat.videoMaxZoomFactor.toFloat().coerceAtMost(5.0f))
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupCamera() {
        captureSession = AVCaptureSession()

        try {
            videoInput = AVCaptureDeviceInput.deviceInputWithDevice(device, null) as AVCaptureDeviceInput
        } catch (e: Exception) {
            onBarcodeFailed(e)
            return
        }

        setupCaptureSession()
    }

    private fun setupCaptureSession() {
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
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            captureSession.startRunning()
        }
    }

    private fun setupMetadataOutput(metadataOutput: AVCaptureMetadataOutput) {
        metadataOutput.setMetadataObjectsDelegate(this, dispatch_get_main_queue())

        val supportedTypes = getMetadataObjectTypes()
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
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            if (!captureSession.isRunning()) {
                captureSession.startRunning()
            }
        }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            if (captureSession.isRunning()) {
                captureSession.stopRunning()
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
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
        metadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .mapNotNull { metadataObject ->
                if (!::previewLayer.isInitialized) return@mapNotNull null
                previewLayer.transformedMetadataObjectForMetadataObject(metadataObject)
                    as? AVMetadataMachineReadableCodeObject
            }
            .filter { barcodeObject ->
                isRequestedFormat(barcodeObject.type)
            }.forEach { barcodeObject ->
                processDetectedBarcode(barcodeObject.stringValue ?: "", barcodeObject.type)
            }
    }

    private fun processDetectedBarcode(
        value: String,
        type: AVMetadataObjectType,
    ) {
        barcodesDetected[value] = (barcodesDetected[value] ?: 0) + 1

        if ((barcodesDetected[value] ?: 0) >= 2) {
            val appSpecificFormat = type.toFormat()
            val barcode =
                Barcode(
                    data = value,
                    format = appSpecificFormat.toString(),
                    rawBytes = stringToRawBytes(value),
                )

            if (!filter(barcode)) return

            onBarcodeSuccess(listOf(barcode))
            barcodesDetected.clear()
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                if (::captureSession.isInitialized && captureSession.isRunning()) {
                    captureSession.stopRunning()
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setZoom(ratio: Float) {
        var locked = false
        try {
            locked = device.lockForConfiguration(null)
            if (locked) {
                val maxZoom = device.activeFormat.videoMaxZoomFactor.toFloat().coerceAtMost(5.0f)
                device.videoZoomFactor = ratio.toDouble().coerceIn(1.0, maxZoom.toDouble())
            }
        } catch (e: Exception) {
            NSLog("Failed to update zoom: %@", e.message ?: "unknown")
        } finally {
            if (locked) device.unlockForConfiguration()
        }
    }

    private fun getMetadataObjectTypes(): List<AVMetadataObjectType> {
        return BarcodeFormatMapper.toAvTypes(codeTypes)
    }

    private fun isRequestedFormat(type: AVMetadataObjectType): Boolean {
        if (codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return BarcodeFormatMapper.isKnownFormat(type)
        }
        val appFormat = BarcodeFormatMapper.toAppFormat(type)
        return codeTypes.contains(appFormat)
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

    private fun AVMetadataObjectType.toFormat(): BarcodeFormat {
        return BarcodeFormatMapper.toAppFormat(this)
    }

    fun dispose() {
        // Stop capture session on background thread to avoid UI unresponsiveness
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            runCatching {
                if (::captureSession.isInitialized) {
                    if (captureSession.isRunning()) captureSession.stopRunning()
                    // Remove inputs/outputs to break potential retain cycles
                    (captureSession.outputs as? List<AVCaptureOutput>)?.forEach { output ->
                        runCatching { captureSession.removeOutput(output) }
                    }
                    (captureSession.inputs as? List<AVCaptureDeviceInput>)?.forEach { input ->
                        runCatching { captureSession.removeInput(input) }
                    }
                }
            }
        }
        // UI cleanup on main thread
        runCatching {
            if (::previewLayer.isInitialized) {
                previewLayer.removeFromSuperlayer()
            }
        }
        barcodesDetected.clear()
    }
}
