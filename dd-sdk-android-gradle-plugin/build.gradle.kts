/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.MavenConfig
import com.datadog.gradle.config.dependencyUpdateConfig
import com.datadog.gradle.config.detektConfig
import com.datadog.gradle.config.jacocoConfig
import com.datadog.gradle.config.javadocConfig
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig
import com.datadog.gradle.config.publishingConfig

plugins {
    // Build
    id("java-gradle-plugin")
    kotlin("jvm")

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    id("com.gradle.plugin-publish") version "0.18.0"

    // Analysis tools
    id("com.github.ben-manes.versions")
    id("io.gitlab.arturbosch.detekt")

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
    testImplementation(libs.kotlinPluginGradle)
    detekt(libs.detektCli)
}

kotlinConfig()
detektConfig()
junitConfig()
jacocoConfig()
javadocConfig()
dependencyUpdateConfig()
publishingConfig("Plugin to upload Proguard/R8 mapping files to Datadog.")

gradlePlugin {
    plugins {
        register("dd-sdk-android-gradle-plugin") {
            id = "com.datadoghq.dd-sdk-android-gradle-plugin" // the alias
            implementationClass = "com.datadog.gradle.plugin.DdAndroidGradlePlugin"
        }
    }
}

pluginBundle {
    website = "https://docs.datadoghq.com/real_user_monitoring/error_tracking/android/"
    vcsUrl = "https://github.com/DataDog/dd-sdk-android-gradle-plugin"
    description = "This plugin is used to upload your Proguard/R8 mapping files to Datadog."

    plugins {
        getByName("dd-sdk-android-gradle-plugin") {
            displayName = "Gradle Plugin for Datadog Android SDK"
            version = MavenConfig.VERSION.name
            tags = mutableListOf("android", "datadog", "mapping", "crash-report", "proguard", "R8")
        }
    }

    mavenCoordinates {
        groupId = MavenConfig.GROUP_ID
        artifactId = "dd-sdk-android-gradle-plugin"
        version = MavenConfig.VERSION.name
    }
}

tasks.withType<Test> {
    dependsOn("pluginUnderTestMetadata")
}
