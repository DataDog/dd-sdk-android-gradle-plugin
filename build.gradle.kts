/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

import com.datadog.gradle.plugin.licenses.tasks.CheckDependencyLicensesTask
import dev.detekt.gradle.Detekt

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
    alias(libs.plugins.detektGradlePlugin)
    alias(libs.plugins.dependencyLicenseGradlePlugin)
    alias(libs.plugins.kotlinGradlePlugin) apply false
    alias(libs.plugins.ktlintGradlePlugin) apply false
    alias(libs.plugins.koverPlugin) apply false
    id("ktlint")
    alias(libs.plugins.dokkaJavadocPlugin) apply false
    alias(libs.plugins.androidApplicationPlugin) apply false
    alias(libs.plugins.androidLibraryPlugin) apply false
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

    // TODO RUM-17809: configuration cache not supported by gradle-dependency-license-plugin; re-enable once fixed
    tasks.withType<CheckDependencyLicensesTask>().configureEach {
        notCompatibleWithConfigurationCache("Accesses Task.project at execution time")
    }
}

val detektSources = files(rootDir)
val detektExcludes = listOf("**/build/**", "**/.gradle/**", "**/.gradle-user-home/**")

tasks.withType<Detekt>().configureEach {
    setSource(detektSources)
    include("**/*.kt", "**/*.kts")
    exclude(detektExcludes)
    jvmTarget = "17"
    reports {
        sarif.required = false
        html.required = false
        checkstyle.required = false
        markdown.required = false
    }
}

tasks.named<Detekt>("detekt") {
    description = "Runs the Detekt checks."
    config.setFrom(rootProject.file("detekt.yml"))
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

tasks.register("ktlintCheckAll") {
    group = "datadog"
    description = "Runs code format check with ktlint for both main and included (`:build-config`) builds."
    dependsOn(allprojects.mapNotNull { it.tasks.named { it == "ktlintCheck" } })
    dependsOn(gradle.includedBuild("build-config").task(":ktlintCheck"))
}

tasks.register("ktlintFormatAll") {
    group = "datadog"
    description = "Runs code formatting with ktlint for both main and included (`:build-config`) builds."
    dependsOn(allprojects.mapNotNull { it.tasks.named { it == "ktlintFormat" } })
    dependsOn(gradle.includedBuild("build-config").task(":ktlintFormat"))
}
