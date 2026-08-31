package org.ncgroup.kscan

/**
 * Web-specific configuration for KScan.
 *
 * Decoding goes through the browser's
 * [BarcodeDetector](https://developer.mozilla.org/docs/Web/API/BarcodeDetector),
 * with a polyfill loaded where the browser has none.
 */
public object KScanWeb {
    /**
     * URL of an ES module that installs a `BarcodeDetector` on `globalThis`.
     *
     * Defaults to the MIT-licensed `barcode-detector` polyfill on jsDelivr. Set
     * your own copy to avoid the CDN; `null` disables the fallback, failing the
     * scan instead of fetching anything.
     */
    public var barcodeDetectorPolyfillUrl: String? =
        "https://cdn.jsdelivr.net/npm/barcode-detector@3.2.2/dist/es/polyfill.js"

    /**
     * URL of the ZXing `.wasm` binary the default polyfill decodes with.
     *
     * Point this at your own copy when you self-host [barcodeDetectorPolyfillUrl].
     * Ignored when the browser has its own `BarcodeDetector`.
     */
    public var zxingWasmUrl: String? =
        "https://cdn.jsdelivr.net/npm/zxing-wasm@3.1.3/dist/reader/zxing_reader.wasm"

    /** Logs the camera resolution and the formats being detected to the console. */
    public var debugLogging: Boolean = false

    /**
     * Id of the camera [ScannerView] opens, from [availableCameras].
     *
     * `null` lets the browser choose, preferring a rear-facing camera.
     */
    public var cameraDeviceId: String? = null
}
