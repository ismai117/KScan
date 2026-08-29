package org.ncgroup.kscan

/**
 * Desktop-specific configuration for KScan.
 *
 * Set [cameraIndex] before [ScannerView] enters the composition to scan with a
 * camera other than the first one the platform reports:
 *
 * ```kotlin
 * KScanDesktop.cameraIndex = 1
 * ```
 *
 * On macOS a nearby iPhone appears here as an ordinary camera through Continuity
 * Camera, so selecting its index scans with the phone and needs nothing else.
 */
public object KScanDesktop {
    /**
     * Index of the camera [ScannerView] opens, counting from zero.
     *
     * Which index maps to which camera is decided by the platform and is not
     * guaranteed to be stable between runs, so treat it as a user choice rather
     * than something to hard-code. An index that cannot be opened is reported as
     * [BarcodeResult.OnFailed], which lets a caller fall back to another.
     */
    public var cameraIndex: Int = 0
}
