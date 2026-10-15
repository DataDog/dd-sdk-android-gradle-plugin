/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.config

import com.datadog.gradle.config.detektConfig
import com.datadog.gradle.config.javadocConfig
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig
import com.datadog.gradle.config.publishingConfig
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import javax.inject.Inject

abstract class BuildConfigExtension @Inject constructor(
    private val project: Project
) {

    fun applyKotlinConfig(
        languageVersion: KotlinVersion = KotlinVersion.KOTLIN_2_3,
        apiVersion: KotlinVersion = KotlinVersion.KOTLIN_2_3,
        // will follow default set by plugin
        coreLibrariesVersion: String? = null
    ) {
        project.kotlinConfig(languageVersion, apiVersion, coreLibrariesVersion)
    }

    fun applyJunitConfig() {
        project.junitConfig()
    }

    fun applyJavadocConfig() {
        project.javadocConfig()
    }

    fun applyPublishingConfig(description: String) {
        project.publishingConfig(description)
    }

    fun applyDetektConfig(ignoredFlavors: List<String> = emptyList()) {
        project.detektConfig(ignoredFlavors)
    }
}
