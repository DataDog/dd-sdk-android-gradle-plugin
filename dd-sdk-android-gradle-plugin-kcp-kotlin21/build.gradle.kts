/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.dependencyUpdateConfig
import com.datadog.gradle.config.jacocoConfig
import com.datadog.gradle.config.javadocConfig
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig
import com.datadog.gradle.config.publishingConfig

buildscript {
    dependencies {
        classpath(libs.kotlinPluginGradle21)
    }
}

plugins {
    id("java-library")

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka-javadoc")
    id("com.vanniktech.maven.publish.base")

    // Analysis tools
    id("com.github.ben-manes.versions")

    // Tests
    jacoco

    // Internal Generation
    id("thirdPartyLicences")
    id("transitiveDependencies")
    id("compilerMetadata")
}

apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
    implementation(project(":dd-sdk-android-gradle-plugin-kcp-common"))
    compileOnly(libs.kotlinCompilerEmbeddable21)
    compileOnly(libs.kotlinReflect)

    testImplementation(testFixtures(project(":dd-sdk-android-gradle-plugin-kcp-common")))
    testImplementation(libs.kotlinCompilerEmbeddable21)
    testImplementation(libs.kotlinCompilerTesting21)
}

java {
    targetCompatibility = JavaVersion.VERSION_11
}

kotlinConfig()
junitConfig()
javadocConfig()
jacocoConfig()
dependencyUpdateConfig()
publishingConfig(
    "Module to support Datadog Compiler Plugin with kotlin 2.1.x",
    false
)
