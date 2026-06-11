/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

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
    id("com.github.ben-manes.versions")

    // Tests
    jacoco

    // Internal Generation
    id("thirdPartyLicences")
    id("transitiveDependencies")
    id("compilerMetadata")
    id("datadogBuildConfig")
}

dependencies {
    implementation(project(":dd-sdk-android-gradle-plugin-kcp-common"))
    compileOnly(libs.kotlinCompilerEmbeddable21)
    compileOnly(libs.kotlinGradlePlugin21)
    compileOnly(libs.autoServiceAnnotation)
    compileOnly(libs.kotlinReflect)
    kapt(libs.autoService)

    testImplementation(testFixtures(project(":dd-sdk-android-gradle-plugin-kcp-common")))
    testImplementation(libs.kotlinCompilerEmbeddable21)
    testImplementation(libs.kotlinGradlePlugin21)
    testImplementation(libs.kotlinCompilerTesting21)
}

java {
    targetCompatibility = JavaVersion.VERSION_11
}

datadogBuildConfig {
    applyKotlinConfig()
    applyJunitConfig()
    applyJavadocConfig()
    applyJacocoConfig()
    applyDependencyUpdateConfig()
    applyPublishingConfig("Module to support Datadog Compiler Plugin with kotlin 2.1.x")
}
