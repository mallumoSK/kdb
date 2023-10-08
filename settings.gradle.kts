pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "kdb"

include(":kdb-core")
include(":kdb-ksp")
include(":test-android")
include(":test-desktop")
