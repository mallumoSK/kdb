import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")// version "1.7.0"
    id("com.google.devtools.ksp") version "1.7.0-1.0.6"
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
    maven("https://mallumo.jfrog.io/artifactory/gradle-dev-local")
    maven("/tmp/___/")
}

dependencies {
    implementation(kotlin("stdlib"))
    ksp(project(":kdb-ksp"))
    implementation(project(":kdb"))
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

}


val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}

