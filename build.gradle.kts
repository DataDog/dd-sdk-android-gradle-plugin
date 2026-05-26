/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

buildscript {
    repositories {
        // Magic Mirror Depot proxy (only set in CI via `.gitlab-ci.yml`).
        listOf("gradlePluginProxy", "mavenRepositoryProxy")
            .mapNotNull { providers.gradleProperty(it).orNull?.takeIf { url -> url.isNotBlank() } }
            .forEach { url -> maven { setUrl(url) } }
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    dependencies {
        // Uncomment to use the samples
        // classpath(libs.datadogPluginGradle)
    }
}

plugins {
    alias(libs.plugins.kotlinPlugin21) apply false
    alias(libs.plugins.dokkaJavadocPlugin) apply false
    alias(libs.plugins.androidApplicationPlugin) apply false
    alias(libs.plugins.androidLibraryPlugin) apply false
    alias(libs.plugins.versionsPluginGradle) apply false
    alias(libs.plugins.mavenPublishPlugin) apply false
}

allprojects {
    repositories {
        // Magic Mirror Depot proxy (only set in CI via `.gitlab-ci.yml`).
        listOf("gradlePluginProxy", "mavenRepositoryProxy")
            .mapNotNull { providers.gradleProperty(it).orNull?.takeIf { url -> url.isNotBlank() } }
            .forEach { url -> maven(url) }
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// Empty task defined by one of our CI pipeline which does not apply here.
tasks.register("checkGeneratedFiles") {
    childProjects.forEach { (name, _) ->
        if (name.startsWith("dd-sdk-android")) {
            dependsOn(":$name:checkTransitiveDependenciesList")
            dependsOn(":$name:checkCompilerMetadataChanges")
        }
    }
}
