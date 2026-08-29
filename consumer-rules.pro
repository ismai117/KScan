# Consumer ProGuard rules for the KScan library
# Packaged with the AAR and applied by consuming apps in release builds.

# ML Kit vision barcode (keep internals referenced via reflection)
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_vision_barcode**

# CameraX core and interop
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
