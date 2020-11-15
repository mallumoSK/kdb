import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
}

group = "tk.mallumo"
version = "0.0.3"

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(30)
    }
    sourceSets {
        val main by getting {
            java.srcDirs("src/androidMain/kotlin")
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
//            res.srcDirs("src/androidMain/res")
        }
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
            }
        }

        val jvmDesktopMain by getting {
            dependencies {
//                api("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                dependsOn(commonMain)
//                api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
//                api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinSerializationVersion")
//                api("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
//                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
//                api("io.ktor:ktor-client-core-jvm:$ktorVersion")
//                api("io.ktor:ktor-client-logging-jvm:$ktorVersion")
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
//publishing {
//    repositories {
//        maven("/tmp/___/")
//    }
//}