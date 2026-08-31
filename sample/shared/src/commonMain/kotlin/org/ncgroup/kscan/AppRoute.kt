package org.ncgroup.kscan

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Formats : Route

    @Serializable
    data class Scanner(val format: BarcodeFormat) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object ImageScan : Route
}

val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Formats::class, Route.Formats.serializer())
            subclass(Route.Scanner::class, Route.Scanner.serializer())
            subclass(Route.Settings::class, Route.Settings.serializer())
            subclass(Route.ImageScan::class, Route.ImageScan.serializer())
        }
    }
}
