/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java-library")
    kotlin("jvm")
    kotlin("kapt")

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka-javadoc")
    id("com.vanniktech.maven.publish.base")

    // Analysis tools
    id("ktlint")
    alias(libs.plugins.detektGradlePlugin)

    // Tests
    id("org.jetbrains.kotlinx.kover")

    // Internal Generation
    id("transitiveDependencies")
    id("compilerMetadata")
    id("datadogBuildConfig")
}

dependencies {
    implementation(project(":dd-sdk-android-gradle-plugin-kcp-common"))
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.kotlinCompilerEmbeddable20)
    compileOnly(libs.kotlinGradlePlugin20)
    compileOnly(libs.autoServiceAnnotation)
    compileOnly(libs.kotlinReflect20)
    kapt(libs.autoService)

    testImplementation(testFixtures(project(":dd-sdk-android-gradle-plugin-kcp-common")))
    testImplementation(libs.kotlinCompilerEmbeddable20)
    testImplementation(libs.kotlinGradlePlugin20)
    testImplementation(libs.kotlinCompilerTesting20)
    testImplementation(libs.kotlinReflect20)
}

datadogBuildConfig {
    applyKotlinConfig(
        languageVersion = KotlinVersion.KOTLIN_2_0,
        apiVersion = KotlinVersion.KOTLIN_2_0,
        coreLibrariesVersion = libs.versions.kotlin20.get()
    )
    applyJunitConfig()
    applyJavadocConfig()
    applyPublishingConfig("Module to support Datadog Compiler Plugin with kotlin 2.0.x or below")
    applyDetektConfig()
}
