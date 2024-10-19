plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.ksp)
    application
}

group = "tk.mallumo"
version = "1.0"

dependencies {
    implementation(project(":kdb-core"))
    ksp(project(":kdb-ksp"))
    implementation(libs.kdb.sqlite)
    implementation(libs.kotlin.coroutines)
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

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

ksp.arg("commonSourcesOnly", "true")
