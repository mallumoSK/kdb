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
            kotlinOptions.jvmTarget = "11"
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
//                api("com.google.code.gson:gson:${toolkit["version.gson"]}")
                api("tk.mallumo:log:${toolkit["version.log"]}")
                api("tk.mallumo:utils:${toolkit["version.utils"]}")
            }
        }
        @Suppress("UNUSED_VARIABLE") val desktopMain by getting
        @Suppress("UNUSED_VARIABLE") val androidMain by getting
    }
}

android {
    compileSdkVersion(31)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(31)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lintOptions.isAbortOnError = false
    lintOptions.isCheckReleaseBuilds = false
    lintOptions.disable("TypographyFractions", "TypographyQuotes")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

apply("../secure.gradle")