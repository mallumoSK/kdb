import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
}

group = "tk.mallumo"
version = "1.0.0"

android {
    compileSdkVersion(30)
//    buildToolsVersion = "30.0.2"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
    sourceSets {
        val main by getting {
            java.srcDirs("src/androidMain/kotlin")
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
//            res.srcDirs("src/androidMain/res")
        }
    }
    configurations {
        create("testApi")
        create("testDebugApi")
        create("testReleaseApi")
    }
}

kotlin {
    jvm("jvmDesktop")
    android {
        publishLibraryVariants("release", "debug")
        publishLibraryVariantsGroupedByFlavor = true
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
            }
        }

        val jvmDesktopMain by getting {
            dependencies {
                dependsOn(commonMain)
            }
        }


        val androidMain by getting {
            dependsOn(commonMain)
        }

        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
    }

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.6"
    }
}
apply("../secure.gradle")