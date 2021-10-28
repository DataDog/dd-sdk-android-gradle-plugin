/*
 * Unless explicitly stated otherwise all pomFilesList in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

import com.datadog.gradle.config.dependencyUpdateConfig
import com.datadog.gradle.config.detektConfig
import com.datadog.gradle.config.jacocoConfig
import com.datadog.gradle.config.javadocConfig
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig
import com.datadog.gradle.config.ktLintConfig
import com.datadog.gradle.config.publishingConfig

plugins {
    // Build
    id("java-gradle-plugin")
    kotlin("jvm")

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka")

    // Analysis tools
    id("com.github.ben-manes.versions")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")

    // Tests
    jacoco

    // Internal Generation
    id("thirdPartyLicences")
    id("transitiveDependencies")
}

dependencies {
    implementation(libs.kotlin)
    implementation(libs.okHttp)
    implementation(libs.json)
    // because auto-wiring into Android projects
    compileOnly(libs.androidToolsPluginGradle)

    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.okHttpMock)
    testImplementation(libs.androidToolsPluginGradle)

    detekt(libs.detektCli)
}

kotlinConfig()
detektConfig()
ktLintConfig()
junitConfig()
jacocoConfig()
javadocConfig()
dependencyUpdateConfig()
publishingConfig("Plugin to upload Proguard/R8 mapping files to Datadog.")

gradlePlugin {
    plugins {
        register("dd-sdk-android-gradle-plugin") {
            id = "dd-sdk-android-gradle-plugin" // the alias
            implementationClass = "com.datadog.gradle.plugin.DdAndroidGradlePlugin"
        }
    }
}

tasks.withType<Test> {
    dependsOn("pluginUnderTestMetadata")
}
