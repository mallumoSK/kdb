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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

apply("../secure-ksp.gradle")

