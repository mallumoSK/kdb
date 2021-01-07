pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "symbol-processing") {
                useModule("com.google.devtools.ksp:symbol-processing:${requested.version}")
            }
        }
    }

    repositories {
        gradlePluginPortal()
        google()
    }
}

include(":kdb-ksp")
include(":kdb")
include(":app")
rootProject.name = "kdb"