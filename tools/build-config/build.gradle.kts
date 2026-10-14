/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import dev.detekt.gradle.Detekt

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlintGradlePlugin)
    alias(libs.plugins.detektGradlePlugin)
}

detekt {
    config.setFrom(file("../../detekt.yml"))
    basePath = layout.projectDirectory.dir("../..")
}

tasks.withType<Detekt>().configureEach {
    val buildDirectory = layout.buildDirectory
    exclude { it.file.startsWith(buildDirectory.get().asFile) }
    reports {
        sarif.required = false
        html.required = false
        checkstyle.required = false
        markdown.required = false
    }
}

// Mirrors `applyDetektConfig`, which can't be used in the build script of this build:
// sources are only analyzed with type resolution, so the plain task is just an umbrella
tasks.named<Detekt>("detekt") {
    enabled = false
    dependsOn(tasks.named { it in setOf("detektMain", "detektTest") })
}

kotlin {
    // Must be kept in sync with JDK_VERSION in com.datadog.gradle.config.KotlinConfig.
    jvmToolchain(21)
}

ktlint {
    version = provider { libs.versions.ktlint.get() }
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated") }
        exclude { it.file.invariantSeparatorsPath.contains("/build/kspCaches/") }
    }
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
    compileOnly(libs.kotlinGradlePluginApi)
    compileOnly(libs.ktlintGradlePlugin)
    compileOnly(libs.detektGradlePlugin)
    compileOnly(libs.dokkaGradlePlugin)
    compileOnly(libs.mavenPublishPlugin)

    // Tests
    testImplementation(libs.bundles.jUnit5)
    testImplementation(libs.bundles.testTools)
}

gradlePlugin {
    plugins {
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

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
}
