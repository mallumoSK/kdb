pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "kotlin-ksp",
                "org.jetbrains.kotlin.kotlin-ksp",
                "org.jetbrains.kotlin.ksp" ->
                    useModule("org.jetbrains.kotlin:kotlin-ksp:${requested.version}")
            }
        }
    }

    repositories {
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        google()
    }
}

include(":kdb-ksp")
include(":kdb")
include(":app")
rootProject.name = "kdb"