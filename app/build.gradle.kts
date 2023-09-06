plugins {
    kotlin("jvm") version "1.9.10"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")

    implementation("org.apache.poi:poi:5.2.0")
    implementation("org.apache.poi:poi-ooxml:5.2.0")
    implementation("com.formdev:flatlaf:3.0")
}

group = "in.toolsuite"
version = "0.0.1-SNAPSHOT"
description = "Toolsuite"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(13))
    }
}

application {
    mainClass.set("core.App")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "core.App"
    }
}