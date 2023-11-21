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

    implementation("org.apache.logging.log4j:log4j-api:2.21.1")
    implementation("org.apache.logging.log4j:log4j-core:2.21.1")
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")
    implementation("com.formdev:flatlaf:3.2.5")
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