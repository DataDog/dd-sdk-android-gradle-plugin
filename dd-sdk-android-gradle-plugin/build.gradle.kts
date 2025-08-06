/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.config.MavenConfig
import com.datadog.gradle.config.dependencyUpdateConfig
import com.datadog.gradle.config.jacocoConfig
import com.datadog.gradle.config.javadocConfig
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig
import com.datadog.gradle.config.publishingConfig

plugins {
    // Build
    id("java-gradle-plugin")
    kotlin("jvm")
    kotlin("kapt")

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
}

// Creating the source sets for different Kotlin versions for compiler plugin compatibility.
// Kotlin 2.0.x versions
val kotlin20: SourceSet by sourceSets.creating
// Kotlin 2.1.x versions
val kotlin21: SourceSet by sourceSets.creating
// Kotlin 2.2.x versions
val kotlin22: SourceSet by sourceSets.creating

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
    testImplementation(kotlin20.output)
    testImplementation(kotlin21.output)
    testImplementation(kotlin22.output)
    testImplementation(libs.kotlinCompilerTesting)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui)

    compileOnly(libs.kotlinPluginGradle)
    kotlin20.compileOnlyConfigurationName(libs.kotlinCompilerEmbeddable20)
    kotlin21.compileOnlyConfigurationName(libs.kotlinCompilerEmbeddable21)
    kotlin22.compileOnlyConfigurationName(libs.kotlinCompilerEmbeddable22)
    compileOnly(libs.kotlinCompilerEmbeddable)
    compileOnly(kotlin20.output)
    compileOnly(kotlin21.output)
    compileOnly(kotlin22.output)
    compileOnly(libs.autoServiceAnnotation)
    kapt(libs.autoService)
}

kotlinConfig()
junitConfig()
jacocoConfig()
javadocConfig()
dependencyUpdateConfig()
publishingConfig("Plugin to upload Proguard/R8 mapping files to Datadog.")

tasks.withType<Jar>().configureEach {
    from(kotlin20.output)
    from(kotlin21.output)
    from(kotlin22.output)
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
