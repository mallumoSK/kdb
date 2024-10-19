plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.kotlin.ksp)
}

android {
    defaultConfig {
        applicationId = "tk.mallumo.test.android"
        namespace = "tk.mallumo.test.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        compileSdk = libs.versions.android.targetSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}


dependencies {
    implementation(libs.androidx.core)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")

    implementation(libs.kotlin.coroutines)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
}

dependencies {
    ksp(project(":kdb-ksp"))
    implementation(project(":kdb-core"))
}
