/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

internal fun Project.kotlinConfig(
    languageVersion: KotlinVersion,
    apiVersion: KotlinVersion,
    coreLibrariesVersion: String
) {
    extensions.getByType<KotlinJvmExtension>().apply {
        this.coreLibrariesVersion = coreLibrariesVersion
        compilerOptions {
            this.languageVersion.set(languageVersion)
            this.apiVersion.set(apiVersion)
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
