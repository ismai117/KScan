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
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.videoZoomFactor
import platform.Foundation.NSLog
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIInterfaceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIViewController
import platform.darwin.dispatch_get_main_queue

/**
 * A UIViewController that manages the camera preview and barcode scanning.
 *
 * @property device The AVCaptureDevice to use for capturing video.
 * @property codeTypes A list of BarcodeFormat types to detect.
 * @property onBarcodeSuccess A callback function that is invoked when barcodes are successfully detected.
 * @property onBarcodeFailed A callback function that is invoked when an error occurs during barcode scanning.
 * @property onBarcodeCanceled A callback function that is invoked when barcode scanning is canceled. (Currently not used within this class)
 * @property filter A callback function that is invoked when barcode result is processed. [onBarcodeSuccess] will only be invoked
 * if the invocation of this property resolves to true.
 * @property onMaxZoomRatioAvailable A callback function that is invoked with the maximum available zoom ratio for the camera.
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
        captureSession.startRunning()
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
        if (!captureSession.isRunning()) {
            captureSession.startRunning()
        }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        if (captureSession.isRunning()) {
            captureSession.stopRunning()
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
                    rawBytes = value.encodeToByteArray(),
                )

            if (!filter(barcode)) return

            onBarcodeSuccess(listOf(barcode))
            barcodesDetected.clear()
            if (::captureSession.isInitialized && captureSession.isRunning()) {
                captureSession.stopRunning()
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
        if (codeTypes.isEmpty() || codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return ALL_SUPPORTED_AV_TYPES
        }

        return codeTypes.mapNotNull { appFormat ->
            APP_TO_AV_FORMAT_MAP[appFormat]
        }
    }

    private fun isRequestedFormat(type: AVMetadataObjectType): Boolean {
        if (codeTypes.contains(BarcodeFormat.FORMAT_ALL_FORMATS)) {
            return AV_TO_APP_FORMAT_MAP.containsKey(type)
        }

        val appFormat = AV_TO_APP_FORMAT_MAP[type] ?: return false

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
        return AV_TO_APP_FORMAT_MAP[this] ?: BarcodeFormat.TYPE_UNKNOWN
    }

    private val AV_TO_APP_FORMAT_MAP: Map<AVMetadataObjectType, BarcodeFormat> =
        mapOf(
            AVMetadataObjectTypeQRCode to BarcodeFormat.FORMAT_QR_CODE,
            AVMetadataObjectTypeEAN13Code to BarcodeFormat.FORMAT_EAN_13,
            AVMetadataObjectTypeEAN8Code to BarcodeFormat.FORMAT_EAN_8,
            AVMetadataObjectTypeCode128Code to BarcodeFormat.FORMAT_CODE_128,
            AVMetadataObjectTypeCode39Code to BarcodeFormat.FORMAT_CODE_39,
            AVMetadataObjectTypeCode93Code to BarcodeFormat.FORMAT_CODE_93,
            AVMetadataObjectTypeUPCECode to BarcodeFormat.FORMAT_UPC_E,
            AVMetadataObjectTypePDF417Code to BarcodeFormat.FORMAT_PDF417,
            AVMetadataObjectTypeAztecCode to BarcodeFormat.FORMAT_AZTEC,
            AVMetadataObjectTypeDataMatrixCode to BarcodeFormat.FORMAT_DATA_MATRIX,
        )

    private val APP_TO_AV_FORMAT_MAP: Map<BarcodeFormat, AVMetadataObjectType> =
        AV_TO_APP_FORMAT_MAP.entries.associateBy({ it.value }) { it.key }

    val ALL_SUPPORTED_AV_TYPES: List<AVMetadataObjectType> = AV_TO_APP_FORMAT_MAP.keys.toList()

    fun dispose() {
        // Best-effort cleanup to avoid retaining camera resources
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
        runCatching {
            if (::previewLayer.isInitialized) {
                previewLayer.removeFromSuperlayer()
            }
        }
        barcodesDetected.clear()
    }
}
