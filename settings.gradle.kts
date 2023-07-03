pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.repsy.io/mvn/mallumo/public")
    }

    infix fun PluginDependencySpec.ofVersion(key: String): PluginDependencySpec = version(extra[key] as String)

    plugins {
        kotlin("multiplatform") ofVersion "version.kotlin"
        kotlin("jvm") ofVersion "version.kotlin"
        kotlin("android") ofVersion "version.kotlin"
        id("com.android.application") ofVersion "version.agp"
        id("com.android.library") ofVersion "version.agp"
        id("com.google.devtools.ksp") ofVersion "version.ksp"
        id("org.jetbrains.kotlinx.binary-compatibility-validator") ofVersion "version.binary.compatibility.validator"
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://repo.repsy.io/mvn/mallumo/public")
    }
}

rootProject.name = "kdb"

include(":kdb")
include(":kdb-ksp")
//include(":test-android")
//include(":test-desktop")
