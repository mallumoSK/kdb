plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "tk.mallumo"
version = "1.0.1"

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:1.4.31-1.0.0-alpha06")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.6"
    }
}
apply("../secure-ksp.gradle")