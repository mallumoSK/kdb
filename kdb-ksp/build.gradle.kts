import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "tk.mallumo"
version = "0.1.0"

dependencies {
    api("com.google.devtools.ksp:symbol-processing:1.4.20-dev-experimental-20201222")
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.6"
    }
}
apply("../secure-ksp.gradle")