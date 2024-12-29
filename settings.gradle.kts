pluginManagement {
    val kotlin_version: String by settings
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
        kotlin("jvm") version kotlin_version
    }
}

rootProject.name = "sad"

include("lib", "script", "app")
