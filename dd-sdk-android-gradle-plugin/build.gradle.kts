/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.MavenConfig

plugins {
    // Build
    id("java-gradle-plugin")
    kotlin("jvm")

    // Publish
    `maven-publish`
    signing
    id("org.jetbrains.dokka-javadoc")
    alias(libs.plugins.gradlePluginPublish)
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
    alias(libs.plugins.buildConfig)
}

buildConfig {
    packageName("com.datadog.gradle.plugin.kcp")
    buildConfigField("DD_PLUGIN_VERSION", MavenConfig.VERSION.name)
}

dependencies {
    // Main implementation dependencies
    implementation(libs.kotlin21)
    implementation(libs.okHttp)
    implementation(libs.json)
    // Compile-only dependencies
    compileOnly(libs.androidToolsGradlePlugin) // for auto-wiring into Android projects
    compileOnly(libs.kotlinCompilerEmbeddable)
    compileOnly(libs.kotlinGradlePlugin21)

    // Test dependencies
    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
    testImplementation(libs.okHttpMock)
    testImplementation(libs.androidToolsGradlePlugin)
    testImplementation(libs.kotlinGradlePlugin21)
}

datadogBuildConfig {
    applyKotlinConfig()
    applyJunitConfig()
    applyJacocoConfig()
    applyJavadocConfig()
    applyDependencyUpdateConfig()
    applyPublishingConfig("Plugin to upload Proguard/R8 mapping files to Datadog.")
}

gradlePlugin {

    website.set("https://docs.datadoghq.com/real_user_monitoring/error_tracking/android/")
    vcsUrl.set("https://github.com/DataDog/dd-sdk-android-gradle-plugin")

    plugins {
        register("dd-sdk-android-gradle-plugin") {
            description = "This plugin is used to upload your Proguard/R8 mapping files to Datadog."
            id = "com.datadoghq.dd-sdk-android-gradle-plugin" // the alias
            implementationClass = "com.datadog.gradle.plugin.DdAndroidGradlePlugin"
            displayName = "Gradle Plugin for Datadog Android SDK"
            version = MavenConfig.VERSION.name
            tags.set(listOf("android", "datadog", "mapping", "crash-report", "proguard", "R8"))
        }
    }
}

// TODO RUMM-3257 Target Java 17 bytecode at some point
java {
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<Test> {
    dependsOn("pluginUnderTestMetadata")
}
