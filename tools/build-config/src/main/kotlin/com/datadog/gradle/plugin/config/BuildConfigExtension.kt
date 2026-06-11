/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.config

import com.datadog.gradle.config.dependencyUpdateConfig
import com.datadog.gradle.config.jacocoConfig
import com.datadog.gradle.config.javadocConfig
import com.datadog.gradle.config.junitConfig
import com.datadog.gradle.config.kotlinConfig
import com.datadog.gradle.config.publishingConfig
import org.gradle.api.Project
import javax.inject.Inject

abstract class BuildConfigExtension @Inject constructor(
    private val project: Project
) {

    fun applyKotlinConfig() {
        project.kotlinConfig()
    }

    fun applyJunitConfig() {
        project.junitConfig()
    }

    fun applyJacocoConfig() {
        project.jacocoConfig()
    }

    fun applyJavadocConfig() {
        project.javadocConfig()
    }

    fun applyDependencyUpdateConfig() {
        project.dependencyUpdateConfig()
    }

    fun applyPublishingConfig(description: String) {
        project.publishingConfig(description)
    }
}
