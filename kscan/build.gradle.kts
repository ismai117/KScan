import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kscan.kmp.library")
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidLibrary {
        namespace = "org.ncgroup.kscan"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.android.mlkitBarcodeScanning)
            implementation(libs.bundles.camera)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
        }
        commonTest.dependencies {
            implementation(libs.compose.ui.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.ismai117", "KScan", "0.7.0")

    pom {
        name.set(project.name)
        description.set("Compose Multiplatform Barcode Scanning Library")
        inceptionYear.set("2024")
        url.set("https://github.com/ismai117/KScan/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("ismai117")
                name.set("ismai117")
                url.set("https://github.com/ismai117/")
            }
        }
        scm {
            url.set("https://github.com/ismai117/KScan/")
            connection.set("scm:git:git://github.com/ismai117/KScan.git")
            developerConnection.set("scm:git:ssh://git@github.com/ismai117/KScan.git")
        }
    }
}
