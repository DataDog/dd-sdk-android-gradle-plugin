/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.noop

import org.gradle.api.Plugin
import org.gradle.api.Project

class BuildConfigClasspathPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // does nothing, exists just to reference it, so that we can bring build classpath -> AndroidConfig
    }
}
