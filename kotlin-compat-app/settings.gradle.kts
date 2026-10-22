/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

// This build is intentionally isolated from the plugin build: it must not reference anything
// outside of this folder, so that no Kotlin version or build configuration can leak in.

pluginManagement {
    val kotlinVersion = providers.gradleProperty("kotlinVersion").orNull
        ?: error("Pass -PkotlinVersion=<Kotlin version to check>")
    val ddPluginVersion = providers.gradleProperty("ddPluginVersion").orNull
        ?: error("Pass -PddPluginVersion=<Datadog Gradle plugin version published to mavenLocal>")

    repositories {
        // Magic Mirror Depot proxy (only set in CI via `.gitlab-ci.yml`).
        listOf("gradlePluginProxy", "mavenRepositoryProxy")
            .mapNotNull { providers.gradleProperty(it).orNull?.takeIf { url -> url.isNotBlank() } }
            .forEach { url -> maven(url) }
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
        id("com.datadoghq.dd-sdk-android-gradle-plugin") version ddPluginVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        listOf("gradlePluginProxy", "mavenRepositoryProxy")
            .mapNotNull { providers.gradleProperty(it).orNull?.takeIf { url -> url.isNotBlank() } }
            .forEach { url -> maven(url) }
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "kotlin-compat-app"
