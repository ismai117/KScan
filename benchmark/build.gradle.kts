plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "org.ncgroup.kscan.benchmark"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        // Higher than the library's own floor: this module is a development tool
        // that is never published, and DEX 040 is what lets its tests keep the
        // spaced-out names the rest of the project's tests use.
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Lets the run write its report where AGP collects it, into
        // build/outputs/connected_android_test_additional_output.
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main").kotlin.srcDir("src/main/kotlin")
        getByName("test").kotlin.srcDir("src/test/kotlin")
        getByName("androidTest").kotlin.srcDir("src/androidTest/kotlin")
    }
}

dependencies {
    // The corpus is generated rather than checked in, so the writers are a main
    // dependency; the reader is only used by the host-side baseline.
    implementation(libs.zxing.core)

    testImplementation(libs.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.storage)
    androidTestUtil(libs.androidx.test.services)
    androidTestImplementation(libs.android.mlkitBarcodeScanning)
    androidTestImplementation(libs.android.zxingcpp)

    // Checks the migrated library through its own public API, not just the decoder.
    androidTestImplementation(project(":kscan"))
}
