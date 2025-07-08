/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import com.datadog.gradle.Dependencies
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.math.BigDecimal
import java.nio.file.Paths

fun Project.jacocoConfig() {
    val jacocoTestReport = tasks.getByName("jacocoTestReport", JacocoReport::class)
    jacocoTestReport.reports {
        csv.required.set(false)
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html"))
    }

    val jacocoTestCoverageVerification = tasks.getByName(
        "jacocoTestCoverageVerification",
        JacocoCoverageVerification::class
    )
    jacocoTestCoverageVerification.violationRules {
        rule {
            limit {
                // TODO increase that when coverage is better?
                minimum = BigDecimal(0.70)
            }
        }
    }

    listOf(
        jacocoTestReport,
        jacocoTestCoverageVerification
    ).forEach { task ->

        val mainSrc = "${project.projectDir}/src/main/kotlin"

        task.executionData.setFrom(
            layout.buildDirectory.files(
                Paths.get("jacoco", "test.exec")
            )
        )
        task.sourceDirectories.setFrom(files(mainSrc))
    }
    jacocoTestReport.dependsOn("test")
    jacocoTestCoverageVerification.dependsOn(jacocoTestReport)

    extensionConfig<JacocoPluginExtension> {
        toolVersion = Dependencies.Versions.Jacoco
        reportsDirectory.set(layout.buildDirectory.dir("jacoco")) // Jacoco's output root.
    }

    tasks.named("check") {
        dependsOn(jacocoTestReport)
        dependsOn(jacocoTestCoverageVerification)
    }

    tasks.named("test") {
        finalizedBy(
            jacocoTestReport,
            jacocoTestCoverageVerification
        )
    }
}
