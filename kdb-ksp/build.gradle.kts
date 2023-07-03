import java.util.*

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

val toolkit by lazy {
    Toolkit.get(extensions = extensions.extraProperties)
}

group = "tk.mallumo"
version = toolkit["version.kdb"]

dependencies {
    api("com.google.devtools.ksp:symbol-processing-api:${toolkit["version.ksp"]}")
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
            artifactId = "kdb-ksp"
            version = toolkit["version.kdb"]
        }
    }
}


