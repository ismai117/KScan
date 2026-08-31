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
     * Which index names which camera is decided by the platform and shifts as
     * devices are plugged and unplugged, so this is a preference rather than a
     * guarantee: when the index cannot be opened, the first other one that will
     * is used instead. [BarcodeResult.OnFailed] is reported only when no camera
     * opens at all.
     */
    public var cameraIndex: Int = 0
}
