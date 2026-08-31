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

**Web** - Uses the browser's [BarcodeDetector API](https://developer.mozilla.org/docs/Web/API/BarcodeDetector), falling back to a polyfill loaded from a CDN.

## Permissions
**Android, iOS, macOS** - Before displaying the `ScannerView`, your application must request and be granted camera permissions by the operating system. On iOS & macOS, add this to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Camera access is required for barcode scanning</string>
```

## Usage

`ScannerView` draws the camera preview and reports what it decodes. It draws nothing else: the torch button, the zoom control, the close affordance and any overlay are yours to build around it, and `modifier` sizes and places the preview.

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

### Choosing a Camera

On web and desktop, `availableCameras()` lists what can be opened and `cameraId` picks
one. `null`, the default, leaves the choice to the platform. Changing it reopens the
camera in place, so a picker works without remounting the view.

```kotlin
var cameras by remember { mutableStateOf(emptyList<CameraDevice>()) }
var cameraId by remember { mutableStateOf<String?>(null) }

LaunchedEffect(Unit) { cameras = availableCameras() }

ScannerView(
    codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
    cameraId = cameraId,
) { result ->
    // handle result
}

cameras.forEach { device ->
    FilterChip(
        selected = cameraId == device.id,
        onClick = { cameraId = device.id },
        label = { Text(device.label) },
    )
}
```

**Web** - labels are blank until the user has granted camera access, so call
`availableCameras()` again once a scan has started to show a meaningful picker. On macOS
a nearby iPhone appears here through Continuity Camera.

**Desktop** - OpenCV cannot be asked what cameras exist, so each index is opened and
closed to find out and the labels are positions, not names. It is slow and lights the
camera indicator, so call it once and hold the result. A camera already in use — by the
scanner or another app — does not appear.

**Android and iOS** ignore `cameraId` and always use the rear camera.

### Web Configuration

Decoding on the web goes through the browser's `BarcodeDetector`, with a polyfill loaded
from a CDN for browsers without one. Self-host it, or set either to `null` to disable
the fallback:

```kotlin
KScanWeb.barcodeDetectorPolyfillUrl = "/assets/barcode-detector.js"
KScanWeb.zxingWasmUrl = "/assets/zxing_reader.wasm"
KScanWeb.debugLogging = true // logs the camera resolution and formats to the console
```

The preview is an HTML element the browser stacks above the Compose canvas, so put
controls beside it rather than over it, and swap it out rather than drawing a result
over it.

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
    }
}
```

## Supported Formats

| Format | Android | iOS | Desktop | Web |
|--------|:-------:|:---:|:-------:|:---:|
| QR_CODE | ✅ | ✅ | ✅ | ✅ |
| AZTEC | ✅ | ✅ | ✅ | ✅ |
| DATA_MATRIX | ✅ | ✅ | ✅ | ✅ |
| PDF417 | ✅ | ✅ | ✅ | ✅ |
| CODE_128 | ✅ | ✅ | ✅ | ✅ |
| CODE_39 | ✅ | ✅ | ✅ | ✅ |
| CODE_93 | ✅ | ✅ | ✅ | ✅ |
| CODABAR | ✅ | ✅ | ✅ | ✅ |
| EAN_13 | ✅ | ✅ | ✅ | ✅ |
| EAN_8 | ✅ | ✅ | ✅ | ✅ |
| ITF | ✅ | ✅ | ✅ | ✅ |
| UPC_A | ✅ | ❌ | ✅ | ✅ |
| UPC_E | ✅ | ✅ | ✅ | ✅ |

Use `BarcodeFormat.FORMAT_ALL_FORMATS` to scan every format the platform supports.

**iOS**

UPC_A has no symbology of its own in AVFoundation or Vision, so it cannot be asked for by name. It is a formal subset of EAN_13, and from the camera AVFoundation reports one as `FORMAT_EAN_13` carrying a leading zero: `036000291452` arrives as `0036000291452`. Ask for `FORMAT_EAN_13` to read one.

CODABAR needs iOS 15.4 for the camera and iOS 15 for `scanImage`.

**Web**

Which of these are available is decided by the browser's `BarcodeDetector`.

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

Run `./gradlew spotlessApply` before pushing. If you change the public API on purpose, run `./gradlew :kscan:updateLegacyAbi` and commit the updated dump in `kscan/api`, or CI will fail.
