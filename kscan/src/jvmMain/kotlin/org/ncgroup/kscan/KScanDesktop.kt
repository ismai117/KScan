package org.ncgroup.kscan

/** Desktop-specific configuration for KScan. */
public object KScanDesktop {
    /**
     * Index of the camera [ScannerView] opens, counting from zero.
     *
     * A preference, not a guarantee: when this index will not open, the first
     * other one that will is used instead.
     */
    public var cameraIndex: Int = 0
}
