/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.config

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class BuildConfigPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create<BuildConfigExtension>(
            EXTENSION_NAME,
            target
        )
    }

    companion object {
        const val EXTENSION_NAME = "datadogBuildConfig"
    }
}
