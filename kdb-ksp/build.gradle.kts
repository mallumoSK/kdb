plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "tk.mallumo"
version = "1.2.1"

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:1.5.10-1.0.0-beta01")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
apply("../secure-ksp.gradle")