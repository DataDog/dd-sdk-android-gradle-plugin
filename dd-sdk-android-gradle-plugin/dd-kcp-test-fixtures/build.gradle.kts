/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

plugins {
    id("java-library")
    kotlin("jvm") version "2.0.21"
}

val pluginVersion: String by extra

group = "com.datadoghq"
version = pluginVersion

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin compiler - compileOnly so each consumer provides their own version
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    
    // Testing framework - exposed as api so consumers get these
    api(platform("org.junit:junit-bom:5.9.3"))
    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core:3.18.1")
    api("org.mockito.kotlin:mockito-kotlin:5.0.0")
    api("org.mockito:mockito-junit-jupiter:5.3.1")
}
