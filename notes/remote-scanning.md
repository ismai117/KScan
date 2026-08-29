# Desktop camera selection vs. phone-as-scanner

Working notes. Two features that sound alike and are not.

**The one-line difference:** in the first, the phone is a *camera*. In the
second, the phone is a *scanner*.

---

## Shipped — "use any camera the platform offers"

```kotlin
KScanDesktop.cameraIndex = 1
```

|                        |                                                                      |
| ---------------------- | -------------------------------------------------------------------- |
| Where the camera is    | anywhere the OS exposes it — built-in, USB, or an iPhone via Continuity Camera |
| Where frames go        | to the desktop                                                        |
| Where decoding happens | desktop (JavaCV → ZXing)                                              |
| Where the viewfinder is| **desktop**                                                           |
| Torch / zoom           | not available — the phone is a passive capture device                 |
| What crosses the wire  | video frames, over Apple's link, invisible to us                      |
| Needs                  | nothing: no app, no pairing, no network code                          |
| Cost                   | ~15 lines                                                             |

Best for a **desk-bound** workflow where the webcam is the bottleneck —
cataloguing, counter work, invoices. You are at the machine anyway.

### Why this was worth doing on its own merits

It is not really an iPhone feature. `ScannerView` on desktop was hardcoded to
`OpenCVFrameGrabber(0)` with no way to reach any other camera, and desktops
routinely have a built-in webcam plus a USB one. Continuity Camera falls out of
fixing that gap for free on macOS.

### What was measured

On this machine, with an iPhone nearby:

```
system_profiler SPCameraDataType
  FaceTime HD Camera
  Ismail's iPhone Camera   (iPhone17,5)      ← Continuity Camera, live
```

JavaCV agrees there are two devices:

```
OpenCV: out device of bound (0-1): 2
index 0: failed
index 1: OPENED  1920x1080                   ← the iPhone; its camera activated
```

### Known limits

- **No device names.** `OpenCVFrameGrabber.getDeviceDescriptions()` throws
  `UnsupportedOperationException: "Device enumeration not support by OpenCV."`
  A picker can only show "Camera 0 / Camera 1" unless we either add FFmpeg
  (tens of MB) or read names from the OS per platform.
- **Index order is not stable.** Documented as a user choice, never hard-coded.
  Index 0 failed to open here while 1 worked, so a picker should try and fall
  back rather than assume.
- **The iPhone case is macOS-only.** Windows and Linux have no equivalent.

### Running it

```
./gradlew :sample:desktopApp:run -Pkscan.camera=1
```

`-P`, not `-D`: a `-D` on the Gradle command line sets a property on the daemon,
not on the application it forks.

If a camera will not open, suspect macOS camera permission before suspecting
KScan. Gradle's `run` launches a bare `java` process, so macOS attributes the
request to the terminal. `runDistributable` runs the packaged bundle instead,
which has its own identity and the `NSCameraUsageDescription` already set in
`nativeDistributions`.

---

## Gap — "turn a phone into a scanner for your desktop"

```
Phone:   camera → decode → "5012345678900"
                                ↓  transport
Desktop: receives result
```

|                        |                                                      |
| ---------------------- | ---------------------------------------------------- |
| Where the camera is    | the phone                                            |
| Where frames go        | **nowhere** — they never leave the phone             |
| Where decoding happens | **phone** (KScan Android/iOS, already works)         |
| Where the viewfinder is| **phone**                                            |
| Torch / zoom           | on the phone, via `ScannerController` — already works|
| What crosses the wire  | one decoded string                                   |
| Needs                  | transport, discovery, pairing + auth, lifecycle      |
| Cost                   | a networking product                                 |

Best for **mobile** workflows — warehouse picking, stock takes, ticketing.
This is the more commonly requested feature in business apps.

---

## Why they are different projects

The scanning half of the gap is **already built**. KScan decodes on Android and
iOS today, with torch and zoom. Everything missing is non-scanning:

- **discovery** — which desktop? mDNS/Bonjour, or a typed-in IP
- **pairing and auth** — without it, anyone on the LAN can inject a barcode into
  your till
- **lifecycle** — reconnect, sleep, app backgrounding, firewalls

That is a networking product wearing a scanning library's name, and it is the
part that would consume maintenance time indefinitely.

So: **`kscan-remote`, a separate artifact.** Opt-in dependency, core stays
small. Otherwise every Android consumer pays — in weight, API surface and
maintenance — for a networking stack they will never call. Same trade as
removing the 36 MB icon dependency for five icons.

### The browser shortcut does not work

Serving a page from the desktop so the phone needs no app install runs into
`getUserMedia`'s **secure-context requirement**. `http://192.168.1.42:8080` is
not a secure context and the browser refuses camera access outright. This is
already the error in our own web code:

```
"Camera access requires a secure context (https or localhost)"
```

localhost is exempt; the phone is not localhost. Ways round it, none free:

- **self-signed cert on the desktop** — works after a scary warning, per device,
  again whenever the cert changes
- **hosted HTTPS relay** — clean UX, but KScan becomes a service with servers to
  operate
- **native companion app** — no browser constraint, but an App Store presence to
  build and maintain

### The optional seam in core

If `kscan-remote` ships, core could expose a source abstraction so a remote
result is indistinguishable from a local scan:

```kotlin
ScannerView(source = BarcodeSource.Remote(resultFlow))
```

An interface and some wiring, not a network stack. But be honest about the
value: once the phone has decoded the string, the desktop app **already has
it**. Routing it back through KScan is ergonomics, not capability. Only build
the seam if something ships behind it.

---

## The thing worth questioning first

If the goal is *real-world desktop scanning*, phones are not what people use.
Retail and warehouse desktops overwhelmingly use **USB HID barcode scanners** —
the gun that acts as a keyboard. That is squarely in our domain, needs no
networking, and is genuinely fiddly to do well: telling scanner input from
typing, prefix/suffix configuration, inter-character timing, focus management.

That may be a better "KScan supports real desktop scanning" story than remote
phones, and it fits the identity we already have: *any scanning device the
platform exposes*.

---

## Open decisions

- [ ] Do we want device **names** on desktop, and is that worth FFmpeg or
      per-platform OS calls?
- [ ] Is `kscan-remote` a real project, or is documenting the pattern enough?
      A developer can already scan on the phone and send the string themselves.
- [ ] USB HID scanners before, after, or instead of remote phones?
- [ ] If `kscan-remote` happens: transport (WebSocket? gRPC?), discovery
      (mDNS?), and how pairing is authenticated.
