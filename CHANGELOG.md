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
  camera in place. Web and desktop; Android and iOS ignore it, as they do `autoZoom`.

### Changed

- **Breaking.** `ScannerView` no longer draws a UI. It renders the camera preview and
  reports what it decodes; controls and overlays are the caller's to build.
- **Breaking.** `ScannerView`'s parameter order is now `codeTypes`, `modifier`, then
  `cameraId`, so positional arguments after `modifier` shift by one.
- **Breaking.** `Barcode.format` is a `BarcodeFormat` rather than a `String`, so it can
  be matched against the enum instead of compared as text. Call `.name` for the old value.
- `modifier` is applied on every platform. Android, desktop and web previously replaced
  it with `fillMaxSize()` and iOS appended `fillMaxSize()` to it.

### Removed

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

- The ML Kit detector is closed when scanning stops. Both the camera and the still-image
  path on Android leaked it, the latter once per `scanImage` call.
