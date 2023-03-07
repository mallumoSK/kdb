pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://mallumo.jfrog.io/artifactory/gradle-dev-local")
    }

    infix fun PluginDependencySpec.versionX(key: String): PluginDependencySpec = version(extra[key] as String)

    plugins {
        kotlin("multiplatform") versionX "version.kotlin"
        kotlin("jvm") versionX "version.kotlin"
        kotlin("android") versionX "version.kotlin"
        id("com.android.application") versionX "version.agp"
        id("com.android.library") versionX "version.agp"
        id("com.google.devtools.ksp") versionX "version.ksp"
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
