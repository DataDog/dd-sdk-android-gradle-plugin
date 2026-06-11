/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

plugins {
    `kotlin-dsl`
    alias(libs.plugins.versionsPluginGradle)
}

buildscript {
    repositories {
        // Magic Mirror Depot proxy (only set in CI via `.gitlab-ci.yml`).
        listOf("gradlePluginProxy", "mavenRepositoryProxy")
            .mapNotNull { providers.gradleProperty(it).orNull?.takeIf { url -> url.isNotBlank() } }
            .forEach { url -> maven { setUrl(url) } }
        mavenCentral()
    }
}

repositories {
    // Magic Mirror Depot proxy (only set in CI via `.gitlab-ci.yml`).
    listOf("gradlePluginProxy", "mavenRepositoryProxy")
        .mapNotNull { providers.gradleProperty(it).orNull?.takeIf { url -> url.isNotBlank() } }
        .forEach { url -> maven { setUrl(url) } }
    mavenCentral()
    google()
    maven { setUrl("https://plugins.gradle.org/m2/") }
    maven { setUrl("https://maven.google.com") }
    maven { setUrl("https://jitpack.io") }
}

dependencies {

    // Dependencies used to configure the Gradle plugins
    compileOnly(libs.kotlinGradlePlugin21)
    compileOnly(libs.androidToolsGradlePlugin)
    compileOnly(libs.versionsGradlePlugin)
    compileOnly(libs.dokkaGradlePlugin)
    compileOnly(libs.mavenPublishPlugin)
    implementation(libs.fuzzyWuzzy)

    // Tests
    testImplementation(libs.jUnit4)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.elmyr.core)
    testImplementation(libs.elmyr.inject)
    testImplementation(libs.elmyr.jUnit4)
    testImplementation(libs.elmyr.jvm)
}

gradlePlugin {
    plugins {
        register("thirdPartyLicences") {
            id = "thirdPartyLicences" // the alias
            implementationClass = "com.datadog.gradle.plugin.checklicenses.ThirdPartyLicensesPlugin"
        }
        register("transitiveDependencies") {
            id = "transitiveDependencies" // the alias
            implementationClass = "com.datadog.gradle.plugin.transdeps.TransitiveDependenciesPlugin"
        }
        register("compilerMetadata") {
            id = "compilerMetadata" // the alias
            implementationClass = "com.datadog.gradle.plugin.compilermeta.CompilerMetaPlugin"
        }
        register("datadogBuildConfig") {
            id = "datadogBuildConfig" // the alias
            implementationClass = "com.datadog.gradle.plugin.config.BuildConfigPlugin"
        }
        register("noopBuildConfigClasspath") {
            id = "noopBuildConfigClasspath" // the alias
            implementationClass = "com.datadog.gradle.plugin.noop.BuildConfigClasspathPlugin"
        }
    }
}
