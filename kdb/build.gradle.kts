@file:Suppress("DEPRECATION")

import java.util.*

plugins {
    id("maven-publish")
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
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
    android {
        publishLibraryVariants("release")
        publishLibraryVariantsGroupedByFlavor = true
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE") val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${toolkit["version.coroutines"]}")
            }
        }
        @Suppress("UNUSED_VARIABLE") val desktopMain by getting
        @Suppress("UNUSED_VARIABLE") val androidMain by getting
    }
}

@Suppress("UnstableApiUsage")
android {
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        compileSdk = 33
        minSdk = 21
        targetSdk = 33
        namespace = "tk.mallumo.kdb"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

    }
    lintOptions {
        isCheckReleaseBuilds = false
        isAbortOnError = false
        disable("TypographyFractions", "TypographyQuotes")
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += setOf("TypographyFractions", "TypographyQuotes")
    }
    buildFeatures {
        buildConfig = false
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

val prop = Properties().apply {
    project.rootProject.file("local.properties").reader().use {
        load(it)
    }
}

publishing {
    val rName = prop["repsy.name"] as String
    val rKey = prop["repsy.key"] as String
    repositories {
        maven {
            name = "repsy.io"
            url = uri("https://repo.repsy.io/mvn/${rName}/public")
            credentials {
                username = rName
                password = rKey
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "tk.mallumo"
            artifactId = "kdb"
            version = toolkit["version.kdb"]
        }
    }
}
