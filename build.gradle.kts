plugins {
    kotlin("multiplatform") version Deps.version.kotlin apply false
    kotlin("jvm") version Deps.version.kotlin apply false
    kotlin("android") version Deps.version.kotlin apply false
    id("com.android.library") version Deps.version.agp apply false
    id("com.google.devtools.ksp") version Deps.version.ksp apply false
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}


tasks.register("_publish_local") {
    group = "_"
    dependsOn(":kdb-core:publishToMavenLocal")
    dependsOn(":kdb-ksp:publishToMavenLocal")
}


tasks.register("_publish_remote") {
    group = "_"
    dependsOn(":kdb-core:publish")
    dependsOn(":kdb-ksp:publish")
}
