/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

plugins {
    id("java-library")
    kotlin("jvm") version "2.0.21"
    kotlin("kapt") version "2.0.21"
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
    api("com.datadoghq:dd-kcp-common:$pluginVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:2.0.21")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")
    kapt("com.google.auto.service:auto-service:1.0.1")

    testImplementation("com.datadoghq:dd-kcp-test-fixtures:$pluginVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
}

tasks.test {
    useJUnitPlatform()
}
