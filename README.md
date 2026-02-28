# KScan

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Latest release](https://img.shields.io/github/v/release/ismai117/KScan?color=brightgreen&label=latest%20release)](https://github.com/ismai117/KScan/releases/latest)

A Compose Multiplatform barcode scanning library for Android and iOS.

| Android | iOS |
|---------|-----|
| <img src="https://github.com/user-attachments/assets/9bce6d77-4028-4a45-b4a2-ad78e79cc0cd" height="600" /> | <img src="https://github.com/user-attachments/assets/36900489-dea0-456b-bd17-00fcb49f9701" height="600" /> | 

## Installation

Add the dependency to your `commonMain` source set:

```kotlin
implementation("io.github.ismai117:KScan:0.7.0")
```

## Platform Setup

**Android** - Uses Google ML Kit for barcode scanning.

**iOS** - Uses AVFoundation for camera and barcode scanning. Add this to your `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Camera access is required for barcode scanning</string>
```

## Usage

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

### Without Default UI

```kotlin
ScannerView(
    codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
    scannerUiOptions = null
) { result ->
    // handle result
}
```

### Custom Controls

Use `ScannerController` for torch and zoom:

```kotlin
val scannerController = remember { ScannerController() }

ScannerView(
    codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
    scannerUiOptions = null,
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

## Supported Formats

| 1D Barcodes | 2D Barcodes |
|-------------|-------------|
| CODE_128 | QR_CODE |
| CODE_39 | AZTEC |
| CODE_93 | DATA_MATRIX |
| CODABAR | PDF417 |
| EAN_13 | |
| EAN_8 | |
| ITF | |
| UPC_A | |
| UPC_E | |

Use `BarcodeFormat.FORMAT_ALL_FORMATS` to scan all supported types.

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
