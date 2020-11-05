import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    `java-library`
}

group = "tk.mallumo"
version = "0.0.1"

dependencies {
    api("org.jetbrains.kotlin:kotlin-symbol-processing-api:1.4.0-dev-experimental-20200914")
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.6"
    }
}
apply("../secure-ksp.gradle")

//configure<PublishingExtension> {
//    repositories {
//        maven("/tmp/___/")
//    }
//    publications {
//        register("mavenJava", MavenPublication::class) {
//            from(components["java"])
//        }
//    }
//}