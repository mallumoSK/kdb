plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    kotlin("android")
}

val toolkit by lazy {
    Toolkit.get(extensions = extensions.extraProperties)
}

@Suppress("UnstableApiUsage")
android {
    defaultConfig {
        applicationId = "tk.mallumo.test.android"
        compileSdk = 33
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    sourceSets.apply {
        getByName("debug") {
            java.srcDirs("build/generated/ksp/debug/kotlin")
        }
        getByName("release") {
            java.srcDirs("build/generated/ksp/release/kotlin")
        }
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


dependencies {
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${toolkit["version.coroutines"]}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
}

dependencies {
    ksp(project(":kdb-ksp"))
    implementation(project(":kdb"))
}
