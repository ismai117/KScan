# :benchmark

Measures barcode decoders against each other over a generated corpus. Nothing here
is published; it exists so a decision about which decoder KScan uses can be made
from numbers rather than from impressions.

## The corpus

`BarcodeCorpus` renders each of the thirteen formats KScan exposes under each of
`Conditions.ALL`, giving 468 frames of 1280x720. The conditions are the axes a
hand-held scan actually varies: how far away the symbol is, how it is turned in
and out of the image plane, how it is lit, how sharp it is, and what colours it is
printed in, plus a group that combines them the way a genuinely bad frame does.

Nothing is checked in as an image. The corpus is generated from source on every
run, so it is reviewable as code and reproducible without carrying several hundred
PNGs in the repository.

## Running it

The pure-Java ZXing decoder runs on a JVM and needs no device:

```
./gradlew :benchmark:testDebugUnitTest
```

That also checks the generator itself, and writes `build/reports/benchmark/`.

ML Kit and zxing-cpp both need an Android runtime, so comparing them needs a
device or emulator:

```
./gradlew :benchmark:connectedDebugAndroidTest
```

The report lands in
`build/outputs/connected_android_test_additional_output/`, as `decoders.md` and
`decoders.csv`.

## Reading the results

A decode is scored as one of three outcomes. `HIT` is the expected payload,
`MISS` is nothing at all, and `MISREAD` is some other payload: they are kept apart
because a scanner that quietly returns the wrong value is worse than one that
returns none.

Where decoders spell the same symbol differently they are not scored against each
other for it. A UPC-A read as the EAN-13 that carries a leading zero, a UPC-E read
expanded, and Codabar's start and stop characters are all listed as accepted forms
on the specimen rather than counted as misreads.

## What it is not

The frames are synthetic. They carry no lens distortion, no rolling shutter, no
sensor demosaicing and no compression artefacts, and the degradations are applied
in a fixed order rather than by an optical system. Timings come from whatever
device the run is on; an emulator is not a phone. The numbers are sound for
comparing decoders against each other on identical input, which is what they are
for, and should not be read as absolute field accuracy.
