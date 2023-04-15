pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://mallumo.jfrog.io/artifactory/gradle-dev-local")
    }

    @Suppress("FunctionName")
    infix fun PluginDependencySpec._version(key: String): PluginDependencySpec = version(extra[key] as String)

    plugins {
        kotlin("multiplatform") _version "version.kotlin"
        kotlin("jvm") _version "version.kotlin"
        kotlin("android") _version "version.kotlin"
        id("com.android.application") _version "version.agp"
        id("com.android.library") _version "version.agp"
        id("com.google.devtools.ksp") _version "version.ksp"
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://mallumo.jfrog.io/artifactory/gradle-dev-local")
    }
}

rootProject.name = "kdb"

include(":kdb")
include(":kdb-ksp")
include(":test-android")
include(":test-desktop")
