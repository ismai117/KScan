# KScan

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Latest release](https://img.shields.io/github/v/release/ismai117/KScan?color=brightgreen&label=latest%20release)](https://github.com/ismai117/KScan/releases/latest)

A Compose Multiplatform barcode scanning library for Android, iOS, Desktop and Web.

| Android | iOS | Desktop |
|---------|-----|---------|
| <img src="https://github.com/user-attachments/assets/9bce6d77-4028-4a45-b4a2-ad78e79cc0cd" height="600"/> | <img src="https://github.com/user-attachments/assets/36900489-dea0-456b-bd17-00fcb49f9701" height="600"/> | <img src="https://github.com/user-attachments/assets/d812a038-2a67-416c-a7a4-f1fcd37bd1f5" height="600"/> |

## Installation

Add the dependency to your `commonMain` source set:

```kotlin
implementation("io.github.ismai117:KScan:$version")
```

## Platform Setup

**Android** - Uses Google ML Kit for barcode scanning.

**iOS** - Uses AVFoundation for camera and barcode scanning. 

**Windows / macOS / Linux** - Uses JavaCV for camera and ZXing for barcode scanning.

**Web** - Uses the browser's [BarcodeDetector API](https://developer.mozilla.org/docs/Web/API/BarcodeDetector). Chromium-based browsers implement it natively; elsewhere KScan loads a polyfill from a CDN the first time you scan. Self-host it, or set either to `null` to disable the fallback:

```kotlin
KScanWeb.barcodeDetectorPolyfillUrl = "/assets/barcode-detector.js"
KScanWeb.zxingWasmUrl = "/assets/zxing_reader.wasm"
```

`availableCameras()` lists the cameras the browser will open; pass an id to `KScanWeb.cameraDeviceId` to pick one. The preview is an HTML element the browser stacks above the Compose canvas, so place controls beside it rather than over it.

## Permissions
**Android, iOS, macOS** - Before displaying the `ScannerView`, your application must request and be granted camera permissions by the operating system. On iOS & macOS, add this to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Camera access is required for barcode scanning</string>
```

## Usage

`ScannerView` draws the camera preview and reports what it decodes. It draws
nothing else: the torch button, the zoom control, the close affordance and any
overlay are yours to build around it.

### Basic

```kotlin
ScannerView(
    codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE, BarcodeFormat.FORMAT_EAN_13)
) { result ->
    when (result) {
        is BarcodeResult.OnSuccess -> {
            println("Barcode: ${result.barcode.data}")
        }
        is BarcodeResult.OnFailed -> {
            println("Error: ${result.exception.message}")
        }
        BarcodeResult.OnCanceled -> {
            println("Canceled")
        }
    }
}
```

The preview fills the space it is given, so size and place it with `modifier`:

```kotlin
Column {
    Button(onClick = { showScanner = false }) { Text("Close") }

    ScannerView(
        modifier = Modifier.fillMaxWidth().weight(1f),
        codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
    ) { result ->
        // handle result
    }
}
```

### Custom Controls

Use `ScannerController` for torch and zoom. It is live only while the
`ScannerView` it was passed to is composed. Desktop cameras expose neither, so
`maxZoomRatio` stays at `1f` there.

```kotlin
val scannerController = remember { ScannerController() }

ScannerView(
    codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
    scannerController = scannerController
) { result ->
    // handle result
}

// Torch control
Button(onClick = { scannerController.setTorch(!scannerController.torchEnabled) }) {
    Text("Toggle Torch")
}

// Zoom control
Slider(
    value = scannerController.zoomRatio,
    onValueChange = scannerController::setZoom,
    valueRange = 1f..scannerController.maxZoomRatio
)
```

### Scanning from Images

Use `scanImage` to scan barcodes from static images (gallery, screenshots, downloaded images) instead of the live camera feed:

```kotlin
scanImage(
    imageBytes = imageBytes, // ByteArray of the image (PNG, JPEG)
    codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
    filter = { barcode -> true }, // Optional: filter detected barcodes
) { result ->
    when (result) {
        is BarcodeResult.OnSuccess -> {
            println("Barcode: ${result.barcode.data}")
            println("Format: ${result.barcode.format}")
        }
        is BarcodeResult.OnFailed -> {
            println("Error: ${result.exception.message}")
        }
        BarcodeResult.OnCanceled -> {
            // Not applicable for image scanning
        }
    }
}
```

## Supported Formats

| | Android | iOS | Desktop | Web |
|---|:---:|:---:|:---:|:---:|
| CODE_128, CODE_39, CODE_93 | ✅ | ✅ | ✅ | ✅ |
| EAN_13, EAN_8 | ✅ | ✅ | ✅ | ✅ |
| UPC_E | ✅ | ✅ | ✅ | ✅ |
| ITF | ✅ | camera only | ✅ | ✅ |
| CODABAR | ✅ | ❌ | ✅ | ✅ |
| UPC_A | ✅ | as EAN_13 | ✅ | ✅ |
| QR_CODE, AZTEC, DATA_MATRIX, PDF417 | ✅ | ✅ | ✅ | ✅ |

Use `BarcodeFormat.FORMAT_ALL_FORMATS` to scan every format the platform supports.

iOS decodes the live camera with AVFoundation and still images with Vision, which
recognise slightly different sets: ITF is available to the camera but not to `scanImage`.
AVFoundation reports UPC-A as EAN-13 with a leading zero, so ask for
`FORMAT_EAN_13` and strip it. Web depends on the browser — a native `BarcodeDetector`
may cover less than the polyfill, and KScan fails the scan rather than silently
returning nothing when none of the requested formats are available.

## License

```
Copyright 2024 ismai117

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
```

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

The public API is checked against `kscan/api`. If a change alters it deliberately, run
`./gradlew :kscan:updateLegacyAbi` and commit the updated dump; CI fails on an
undeclared change. Run `./gradlew spotlessApply` before pushing.
