import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    application
    id("com.google.devtools.ksp")
}

group = "tk.mallumo"
version = "1.0"

dependencies {
    implementation(project(":kdb"))
    ksp(project(":kdb-ksp"))
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}


application {
    mainClass.set("test.MainKt")
}
