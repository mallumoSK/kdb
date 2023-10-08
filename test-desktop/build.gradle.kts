plugins {
    kotlin("jvm")
    application
    id("com.google.devtools.ksp")
}

group = "tk.mallumo"
version = "1.0"

dependencies {
    implementation(project(":kdb-core"))
    ksp(project(":kdb-ksp"))
    implementation(Deps.lib.sqliteJdbc)
    implementation(Deps.lib.coroutines)
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
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

ksp.arg("commonSourcesOnly", "true")
