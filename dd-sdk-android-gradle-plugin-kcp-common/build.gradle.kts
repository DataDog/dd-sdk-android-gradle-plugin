/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

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
    id("com.github.ben-manes.versions")

    // Internal Generation
    id("thirdPartyLicences")
    id("transitiveDependencies")
    id("compilerMetadata")
    id("datadogBuildConfig")
}

dependencies {
    compileOnly(libs.kotlinCompilerEmbeddable20)
    compileOnly(libs.kotlinGradlePlugin20)
    compileOnly(libs.autoServiceAnnotation)
    kapt(libs.autoService)

    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.kotlinCompilerEmbeddable20)
    testImplementation(libs.kotlinGradlePlugin20)

    testFixturesImplementation(libs.kotlinCompilerEmbeddable20)
    testFixturesImplementation(libs.kotlinCompilerTesting20)
    testFixturesApi(libs.kotlinGradlePlugin20)
    testFixturesApi(libs.bundles.jUnit5)
    testFixturesApi(libs.bundles.testTools)
    testFixturesApi(platform(libs.androidx.compose.bom))
    testFixturesApi(libs.androidx.ui)
}

java {
    targetCompatibility = JavaVersion.VERSION_17
}

datadogBuildConfig {
    applyJavadocConfig()
    applyKotlinConfig()
    applyJunitConfig()
    applyDependencyUpdateConfig()
    applyPublishingConfig("Common module for Datadog Kotlin Compiler Plugin")
}
