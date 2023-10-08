plugins {
    id("maven-publish")
    kotlin("multiplatform")
    id("com.android.library")
}

group = Deps.group
version = Deps.core.version

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

kotlin {
    jvm() {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        publishLibraryVariantsGroupedByFlavor = true
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(Deps.lib.coroutines)
            }
        }
        val jvmMain by getting
        val androidMain by getting
    }
}

@Suppress("UnstableApiUsage")
android {
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        namespace = "${Deps.group}.${Deps.core.artifact.replace("-",".")}"
        compileSdk = 33
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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


publishing {
    val rName = propertiesLocal["repsy.name"]
    val rKey = propertiesLocal["repsy.key"]
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
}
