plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
}

application {
    mainClass.set("org.modelica.ide.ApplicationKt")
}

dependencies {
    implementation(project(":modelica-compiler"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.cors)
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    implementation(libs.jackson.module.kotlin)
    implementation(libs.logback.classic)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    testImplementation(libs.kotlin.test)
}

ktor {
    fatJar {
        archiveFileName.set("modelica-ide-backend.jar")
    }
}