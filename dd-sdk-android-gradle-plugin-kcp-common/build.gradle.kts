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

plugins {
    id("java-library")
    kotlin("jvm")
    kotlin("kapt")
    `java-test-fixtures`

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka-javadoc")
    id("com.vanniktech.maven.publish.base")

    // Tests
    jacoco

    // Analysis tools
    id("com.github.ben-manes.versions")

    // Internal Generation
    id("thirdPartyLicences")
    id("transitiveDependencies")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.kotlinCompilerEmbeddable20)
    compileOnly(libs.kotlinPluginGradle)
    compileOnly(libs.autoServiceAnnotation)
    kapt(libs.autoService)

    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.kotlinCompilerEmbeddable)
    testImplementation(libs.kotlinCompilerTesting20)
    testImplementation(libs.kotlinPluginGradle)

    testFixturesImplementation(libs.kotlinCompilerEmbeddable20)
    testFixturesImplementation(libs.kotlinCompilerTesting20)
    testFixturesApi(libs.kotlinPluginGradle)
    testFixturesApi(libs.bundles.jUnit5)
    testFixturesApi(libs.bundles.testTools)
    testFixturesApi(platform(libs.androidx.compose.bom))
    testFixturesApi(libs.androidx.ui)
}

java {
    targetCompatibility = JavaVersion.VERSION_11
}

javadocConfig()
kotlinConfig()
junitConfig()
jacocoConfig()
dependencyUpdateConfig()
publishingConfig("Common module for Datadog Kotlin Compiler Plugin", false)
