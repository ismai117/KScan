@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ncgroup.kscan.scanner

import kotlinx.coroutines.await
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.format.BarcodeFormatMapper
import kotlin.js.Promise
import kotlin.js.get

internal external interface DetectedBarcode : JsAny {
    val rawValue: JsString
    val format: JsString
}

private fun hasNativeBarcodeDetector(): Boolean = js("typeof globalThis.BarcodeDetector !== 'undefined'")

private fun importModule(url: String): Promise<JsAny> = js("import(/* webpackIgnore: true */ url)")

private fun canOverrideWasmLocation(module: JsAny): Boolean = js("typeof module.setZXingModuleOverrides === 'function'")

// The polyfill otherwise looks for its wasm next to its own module URL, which is
// not where the CDN serves it.
private fun overrideWasmLocation(module: JsAny, wasmUrl: String): Unit = js(
    """module.setZXingModuleOverrides({
            locateFile: (path, prefix) => path.endsWith('.wasm') ? wasmUrl : prefix + path,
        })""",
)

private fun supportedFormats(): Promise<JsArray<JsString>> = js(
    """(globalThis.BarcodeDetector.getSupportedFormats
            ? globalThis.BarcodeDetector.getSupportedFormats()
            : Promise.resolve([]))""",
)

private fun newBarcodeDetector(formats: JsArray<JsString>): JsAny = js("new globalThis.BarcodeDetector({ formats: formats })")

private fun logInfo(message: String): Unit = js("console.info(message)")

internal suspend fun barcodeDetector(
    formats: List<String>,
    polyfillUrl: String,
    zxingWasmUrl: String,
    debug: Boolean,
): JsAny {
    if (!hasNativeBarcodeDetector()) {
        if (polyfillUrl.isEmpty()) {
            error(
                "This browser has no BarcodeDetector and " +
                    "KScanWeb.barcodeDetectorPolyfillUrl is null",
            )
        }
        if (debug) logInfo("[KScan] no native BarcodeDetector, loading polyfill")

        val polyfill = importModule(polyfillUrl).await()
        if (zxingWasmUrl.isNotEmpty() && canOverrideWasmLocation(polyfill)) {
            overrideWasmLocation(polyfill, zxingWasmUrl)
        }
    }

    // Native implementations cover only a subset — macOS Chrome is Vision-backed
    // and has no Codabar — and passing one a format it does not know can throw or
    // silently yield nothing.
    val supported = supportedFormats().await().toStringList()
    val requested = if (supported.isEmpty()) formats else formats.filter { it in supported }

    if (requested.isEmpty()) {
        error(
            "None of the requested formats are supported by this browser. " +
                "Requested: ${formats.joinToString(",")}. " +
                "Supported: ${supported.joinToString(",")}",
        )
    }

    if (debug) logInfo("[KScan] detecting formats: ${requested.joinToString(",")}")

    return newBarcodeDetector(requested.toJsArray())
}

private fun JsArray<JsString>.toStringList(): List<String> = buildList {
    for (index in 0 until this@toStringList.length) {
        this@toStringList[index]?.let { add(it.toString()) }
    }
}

private fun List<String>.toJsArray(): JsArray<JsString> {
    val array = JsArray<JsString>()
    forEachIndexed { index, value -> array[index] = value.toJsString() }
    return array
}

internal fun detectFrom(detector: JsAny, source: JsAny): Promise<JsArray<DetectedBarcode>> = js("detector.detect(source)")

// Detectors accept an HTMLVideoElement in principle, but implementations vary in
// how well they handle a live one, so the frame is snapshotted first.
internal fun detectFromVideoFrame(
    detector: JsAny,
    video: org.w3c.dom.HTMLVideoElement,
    debug: Boolean,
): Promise<JsArray<DetectedBarcode>> = js(
    """(async () => {
            const bitmap = await createImageBitmap(video);
            if (debug && !video.__kscanLoggedFrame) {
                video.__kscanLoggedFrame = true;
                console.info('[KScan] frame ' + bitmap.width + 'x' + bitmap.height);
            }
            try {
                return await detector.detect(bitmap);
            } finally {
                bitmap.close();
            }
        })()""",
)

// An empty mimeType leaves the browser to identify the format from the bytes.
internal fun imageBitmapFromBase64(data: String, mimeType: String): Promise<JsAny> = js(
    """(async () => {
            const binary = atob(data);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            const blob = mimeType
                ? new Blob([bytes], { type: mimeType })
                : new Blob([bytes]);
            return await createImageBitmap(blob);
        })()""",
)

internal fun closeImageBitmap(bitmap: JsAny): Unit = js("bitmap.close()")

private fun DetectedBarcode.toBarcode(): Barcode {
    val data = rawValue.toString()

    return Barcode(
        data = data,
        format = BarcodeFormatMapper.toAppFormat(format.toString()),
        rawBytes = data.encodeToByteArray(),
    )
}

internal fun JsArray<DetectedBarcode>.toBarcodes(): List<Barcode> = buildList {
    for (index in 0 until this@toBarcodes.length) {
        this@toBarcodes[index]?.let { add(it.toBarcode()) }
    }
}
