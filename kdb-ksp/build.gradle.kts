import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

apply("../secure-ksp.gradle")

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
