import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("kscanSample")
        browser {
            commonWebpackConfig {
                outputFileName = "kscanSample.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(project(":sample:shared"))
            }
        }
    }
}

tasks.register<Sync>("publishToDocs") {
    group = "distribution"
    description = "Builds the web sample and copies it into /docs for GitHub Pages."

    dependsOn("wasmJsBrowserDistribution")

    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(rootProject.layout.projectDirectory.dir("docs"))
}
