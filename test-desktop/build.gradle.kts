plugins {
    kotlin("jvm")
    application
    id("com.google.devtools.ksp")
}

val toolkit by lazy {
    Toolkit.get(extensions = extensions.extraProperties)
}

group = "tk.mallumo"
version = "1.0"

dependencies {
    implementation(project(":kdb"))
    ksp(project(":kdb-ksp"))
    implementation("org.xerial:sqlite-jdbc:${toolkit["version.sqlite.jdbc"]}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${toolkit["version.coroutines"]}")
}

kotlin {
    sourceSets.main {
//        kotlin.srcDir("build/generated/ksp/main/kotlin")
        kotlin.srcDir("build/generated/ksp/common/commonMain/kotlin")
    }
}


application {
    mainClass.set("test.MainKt")
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

ksp.arg("commonSourcesOnly", "true")
