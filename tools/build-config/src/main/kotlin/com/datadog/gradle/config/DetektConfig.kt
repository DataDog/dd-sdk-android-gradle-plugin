/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

// Tasks created by the Detekt plugin which run with type resolution
private val typeResolutionTasks = setOf("detektMain", "detektTest", "detektTestFixtures")

internal fun Project.detektConfig(ignoredFlavors: List<String>) {
    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.file("detekt.yml"))
        // Release variants don't have dedicated sources, analyzing debug ones is enough
        ignoredBuildTypes.add("release")
        this.ignoredFlavors.addAll(ignoredFlavors)
    }

    tasks.withType<Detekt>().configureEach {
        // generated sources (e.g. BuildConfig) are part of the compilations
        val buildDirectory = layout.buildDirectory
        exclude { it.file.startsWith(buildDirectory.get().asFile) }
        reports {
            sarif.required.set(false)
            html.required.set(false)
            checkstyle.required.set(false)
            markdown.required.set(false)
        }
    }

    // Sources are only analyzed with type resolution, so the plain task is just an umbrella
    tasks.named<Detekt>("detekt") {
        enabled = false
        dependsOn(tasks.named { it in typeResolutionTasks })
    }
}
