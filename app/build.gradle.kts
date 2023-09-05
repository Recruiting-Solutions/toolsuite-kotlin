/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    `java-library`
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.apache.poi:poi:5.2.0")
    api("org.apache.poi:poi-ooxml:5.2.0")
    api("org.apache.logging.log4j:log4j-api:2.19.0")
    api("org.apache.logging.log4j:log4j-core:2.19.0")
    api("com.formdev:flatlaf:3.0")
}

group = "in.toolsuite"
version = "0.0.1-SNAPSHOT"
description = "Toolsuite"
java.sourceCompatibility = JavaVersion.VERSION_13

application {
    mainClass.set("core.App")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "core.App"
    }
}