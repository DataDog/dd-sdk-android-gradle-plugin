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
    compileOnly(libs.kotlinCompilerEmbeddable24)
    compileOnly(libs.kotlinGradlePlugin24)
    compileOnly(libs.autoServiceAnnotation)
    kapt(libs.autoService)

    testImplementation(testFixtures(project(":dd-sdk-android-gradle-plugin-kcp-common")))
    testImplementation(libs.kotlinCompilerEmbeddable24)
    testImplementation(libs.kotlinGradlePlugin24)
    testImplementation(libs.kotlinCompilerTesting24)
    testImplementation(libs.kotlinReflect24)
}

datadogBuildConfig {
    // Use 2.3, because KGP plugin is 2.3; 2.4 language/api version is marked as experimental there.
    // Needs to be updated when KGP version moves forward.
    applyKotlinConfig(
        languageVersion = KotlinVersion.KOTLIN_2_3,
        apiVersion = KotlinVersion.KOTLIN_2_3,
        coreLibrariesVersion = libs.versions.kotlin24.get()
    )
    applyJunitConfig()
    applyJavadocConfig()
    applyPublishingConfig("Module to support Datadog Compiler Plugin with kotlin 2.4.x or above")
    applyDetektConfig()
}
