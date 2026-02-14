import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("kscan.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "org.ncgroup.kscan.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            api(project(":kscan"))
        }
        androidMain.dependencies {
            api(libs.androidx.activityCompose)
            api(libs.androidx.appcompat)
            api(libs.androidx.core.ktx)
            api(libs.moko.permissions)
            api(libs.moko.permissions.compose)
            api(compose.preview)
        }
        iosMain.dependencies {
            api(libs.moko.permissions)
            api(libs.moko.permissions.compose)
        }
    }
}
