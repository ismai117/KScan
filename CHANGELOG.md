# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Being pre-1.0, breaking
changes raise the minor version.

## [0.10.0] - Unreleased

### Added

- Web support. The `wasmJs` target existed before but did nothing: `ScannerView` had an
  empty body and `scanImage` always failed. Both are implemented, decoding through the
  browser's `BarcodeDetector` API with a polyfill fallback for browsers without it.
- `KScanWeb` for the polyfill and ZXing wasm URLs, and debug logging.
- `availableCameras()` and `CameraDevice` for listing the cameras that can be opened,
  on web and desktop. Desktop has no enumeration API behind it, so each index is opened
  once to find out and the labels are positions rather than names.
- `ScannerView`'s `cameraId`, taking a `CameraDevice.id`. Changing it reopens the
  camera in place. Web and desktop; Android and iOS ignore it.

### Changed

- Android decodes with [zxing-cpp](https://github.com/zxing-cpp/zxing-cpp) rather than
  Google ML Kit, so every dependency is open source and the library can be used in apps
  published on F-Droid. Measured over 468 generated frames covering every format at
  a range of angles, distances and lighting, zxing-cpp reads 79.9% of them against ML
  Kit's 89.1%, misreads 0.9% against 0.6%, and is about seven times faster. It is ahead on QR, Data Matrix and Aztec, and
  on symbols too small or too distant for ML Kit; it is behind on linear symbols
  rotated off axis, on strongly uneven lighting, and on heavy defocus.
- Android decodes frames on a thread of the library's own rather than on the main
  thread, since zxing-cpp decodes on the thread that calls it. `filter` and `result`
  are still called on the main thread.
- **Breaking.** `ScannerView` no longer draws a UI. It renders the camera preview and
  reports what it decodes; controls and overlays are the caller's to build.
- **Breaking.** `ScannerView`'s parameter order is now `codeTypes`, `modifier`, then
  `cameraId`, so positional arguments after `modifier` shift by one.
- **Breaking.** `Barcode.format` is a `BarcodeFormat` rather than a `String`, so it can
  be matched against the enum instead of compared as text. Call `.name` for the old value.
- `modifier` is applied on every platform. Android, desktop and web previously replaced
  it with `fillMaxSize()` and iOS appended `fillMaxSize()` to it.

### Removed

- **Breaking.** `ScannerView`'s `autoZoom`. It was backed by ML Kit's zoom suggestions,
  which zxing-cpp has no equivalent for, and it never did anything on iOS, desktop or
  web. Drive zoom from `ScannerController` instead. Callers passing it positionally
  should note that `result` has moved up a place.
- **Breaking.** `ScannerColors`, `scannerColors()`, `ScannerUiOptions` and `ScannerUI()`.
- **Breaking.** `ScannerView`'s `colors` and `scannerUiOptions` parameters.
- **Breaking.** `KScanDesktop`. Its only member was `cameraIndex`, replaced by
  `ScannerView`'s `cameraId`.
- **Breaking.** `KScanWeb.cameraDeviceId`, replaced by `ScannerView`'s `cameraId`.
  One-time configuration stays on `KScanWeb`; what the scan uses is a parameter.
- **Breaking.** `BarcodeResult.OnCanceled`. Only Android ever emitted it, and only when
  ML Kit's detection task was cancelled, which KScan never does. A scan now either
  succeeds or fails.

### Fixed

- The still-image path no longer leaks a decoder on every `scanImage` call. That was
  ML Kit's, which Android no longer uses; the reader replacing it holds no native
  handle to close.

