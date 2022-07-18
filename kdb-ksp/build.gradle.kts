plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "tk.mallumo"
version = "1.7.10-1.0.6-1.4.0"

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:1.7.10-1.0.6")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
apply("../secure-ksp.gradle")