plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "tk.mallumo"
version = "1.1.0"

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:1.4.32-1.0.0-alpha07")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.6"
    }
}
apply("../secure-ksp.gradle")