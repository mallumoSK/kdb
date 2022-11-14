plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

val toolkit by lazy {
    Toolkit.get(extensions = extensions.extraProperties)
}

group = "tk.mallumo"
version = toolkit["version.kdb"]

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    android{
        publishLibraryVariants("release")
        publishLibraryVariantsGroupedByFlavor = true
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE") val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${toolkit["version.coroutines"]}")
                api("tk.mallumo:log:${toolkit["version.log"]}")
                api("tk.mallumo:utils:${toolkit["version.utils"]}")
            }
        }
        @Suppress("UNUSED_VARIABLE") val desktopMain by getting
        @Suppress("UNUSED_VARIABLE") val androidMain by getting
    }
}

@Suppress("UnstableApiUsage")
android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
    namespace = "tk.mallumo.kdb"
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += setOf("TypographyFractions", "TypographyQuotes")
    }
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

apply("../secure.gradle")
