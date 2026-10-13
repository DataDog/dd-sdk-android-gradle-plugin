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
    `java-test-fixtures`

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka-javadoc")
    id("com.vanniktech.maven.publish.base")

    // Tests
    id("org.jetbrains.kotlinx.kover")

    // Analysis tools
    id("ktlint")
    alias(libs.plugins.detektGradlePlugin)

    // Internal Generation
    id("transitiveDependencies")
    id("compilerMetadata")
    id("datadogBuildConfig")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.kotlinCompilerEmbeddable20)
    compileOnly(libs.kotlinGradlePlugin20)
    compileOnly(libs.autoServiceAnnotation)
    kapt(libs.autoService)

    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.kotlinCompilerEmbeddable20)
    testImplementation(libs.kotlinGradlePlugin20)

    testFixturesCompileOnly(libs.kotlinCompilerEmbeddable20)
    testFixturesCompileOnly(libs.kotlinCompilerTesting20)
    testFixturesCompileOnly(libs.kotlinGradlePlugin20)
    testFixturesCompileOnly(libs.kotlinReflect20)
    testFixturesApi(libs.bundles.jUnit5)
    testFixturesApi(libs.bundles.testTools)
    testFixturesApi(platform(libs.androidx.compose.bom))
    testFixturesApi(libs.androidx.ui)
}

datadogBuildConfig {
    applyJavadocConfig()
    applyKotlinConfig(
        languageVersion = KotlinVersion.KOTLIN_2_0,
        apiVersion = KotlinVersion.KOTLIN_2_0,
        coreLibrariesVersion = libs.versions.kotlin20.get()
    )
    applyJunitConfig()
    applyPublishingConfig("Common module for Datadog Kotlin Compiler Plugin")
    applyDetektConfig()
}
