# Consumer ProGuard rules for the KScan library
# Packaged with the AAR and applied by consuming apps in release builds.

# zxing-cpp ships its own keep rules for the classes its JNI layer reads, so
# only CameraX needs naming here.

# CameraX core and interop
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
