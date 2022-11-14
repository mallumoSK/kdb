plugins {
    kotlin("jvm")
}

val toolkit by lazy {
    Toolkit.get(extensions = extensions.extraProperties)
}

group = "tk.mallumo"
version = toolkit["version.kdb"]

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:${toolkit["version.ksp"]}")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

apply("../secure-ksp.gradle")

