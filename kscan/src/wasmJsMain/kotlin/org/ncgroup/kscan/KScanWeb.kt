package org.ncgroup.kscan

/**
 * Web-specific configuration for KScan.
 *
 * Barcode decoding on the web goes through the [BarcodeDetector Web API](https://developer.mozilla.org/docs/Web/API/BarcodeDetector).
 * Chromium-based browsers implement it natively. Everywhere else (Safari, Firefox)
 * KScan lazily loads a polyfill from [barcodeDetectorPolyfillUrl] the first time a
 * scan is requested.
 *
 * Set [barcodeDetectorPolyfillUrl] before the first scan to self-host the polyfill
 * instead of pulling it from a CDN, which is required if your page runs under a
 * Content Security Policy that forbids third-party scripts, or if your app must
 * work offline:
 *
 * ```kotlin
 * KScanWeb.barcodeDetectorPolyfillUrl = "/assets/barcode-detector.js"
 * ```
 *
 * Set it to `null` to disable the fallback entirely. Scans in browsers without a
 * native `BarcodeDetector` then fail with [BarcodeResult.OnFailed] rather than
 * fetching anything over the network.
 */
public object KScanWeb {
    /**
     * URL of an ES module that installs a `BarcodeDetector` implementation on
     * `globalThis` when imported.
     *
     * Defaults to the MIT-licensed, ZXing-WASM backed `barcode-detector` polyfill,
     * pinned to an exact version on jsDelivr. `null` disables the fallback.
     */
    public var barcodeDetectorPolyfillUrl: String? =
        "https://cdn.jsdelivr.net/npm/barcode-detector@3.2.2/dist/es/polyfill.js"

    /**
     * URL of the ZXing `.wasm` binary that the default polyfill decodes with.
     *
     * The polyfill resolves this file relative to its own module URL, which does
     * not exist on the CDN, so KScan overrides the location via the polyfill's
     * `setZXingModuleOverrides` export. Point this at your own copy when you
     * self-host [barcodeDetectorPolyfillUrl].
     *
     * Ignored when the browser has a native `BarcodeDetector`, when the fallback
     * is disabled, or when the configured polyfill exposes no such override.
     */
    public var zxingWasmUrl: String? =
        "https://cdn.jsdelivr.net/npm/zxing-wasm@3.1.3/dist/reader/zxing_reader.wasm"

    /**
     * Logs the negotiated camera resolution, the detector implementation in use,
     * and the formats it was given to `console.info`.
     *
     * Off by default. Browser-to-browser differences are the usual cause of a
     * scanner that works in one browser and not another, so turn this on when
     * diagnosing that:
     *
     * ```kotlin
     * KScanWeb.debugLogging = true
     * ```
     */
    public var debugLogging: Boolean = false
}
