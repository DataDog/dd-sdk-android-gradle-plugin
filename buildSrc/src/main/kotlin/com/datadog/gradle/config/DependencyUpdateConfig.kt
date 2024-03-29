/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.Project

fun Project.dependencyUpdateConfig() {
    taskConfig<DependencyUpdatesTask> {
        revision = "release"
    }
}
