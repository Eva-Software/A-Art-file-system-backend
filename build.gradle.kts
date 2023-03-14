object Versions {
    const val logback = "1.4.5"
    const val uuid = "0.0.17"
    const val ktor = "2.2.4"
    const val kotlinXSerialization = "1.4.1"
}

plugins {
    application
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
}

application {
    mainClass.set("MainKt")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "ch.qos.logback", name = "logback-classic", version = Versions.logback)

    implementation(group = "app.softwork", name = "kotlinx-uuid-core", version = Versions.uuid)

    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = Versions.kotlinXSerialization)

    implementation(group = "io.ktor", name = "ktor-server-netty", version = Versions.ktor)
    implementation(group = "io.ktor", name = "ktor-server-resources", version = Versions.ktor)
    implementation(group = "io.ktor", name = "ktor-server-cors", version = Versions.ktor)
    implementation(group = "io.ktor", name = "ktor-serialization-kotlinx-json", version = Versions.ktor)
    implementation(group = "io.ktor", name = "ktor-server-content-negotiation", version = Versions.ktor)
    implementation("io.ktor:ktor-server-cors-jvm:2.2.4")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.2.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}