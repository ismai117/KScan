plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
}

dependencies {
    dokka(project(":kscan"))
}

allprojects {
    apply(plugin = "com.diffplug.spotless")
    spotless {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**")
            ktlint(libs.versions.ktlint.get())
                .customRuleSets(
                    listOf(
                        libs.composeRules.ktlint
                            .get()
                            .toString(),
                    ),
                ).editorConfigOverride(
                    mapOf(
                        "max_line_length" to "off",
                        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                        "compose_allowed_composition_locals" to "LocalScannerModeState",
                    ),
                )
        }

        kotlinGradle {
            target("*.gradle.kts")
            ktlint(libs.versions.ktlint.get())
        }
    }
}

tasks.register<Copy>("setUpGitHooks") {
    group = "help"
    from("$rootDir/.hooks")
    into("$rootDir/.git/hooks")
}
