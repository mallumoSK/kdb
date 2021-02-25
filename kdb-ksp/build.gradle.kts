import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "tk.mallumo"
version = "1.0.0"

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:1.4.30-1.0.0-alpha02")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.6"
    }
}
apply("../secure-ksp.gradle")