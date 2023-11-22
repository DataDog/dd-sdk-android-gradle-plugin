/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

fun Project.kotlinConfig() {
    taskConfig<KotlinCompile> {
        kotlinOptions {
            // TODO RUMM-3257 Target Java 17 bytecode at some point
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

    val moduleName = this@kotlinConfig.name
    val javaAgentJar = File(File(rootDir, "libs"), "dd-java-agent-0.98.1.jar")
    taskConfig<Test> {
        if (environment["DD_INTEGRATION_JUNIT_5_ENABLED"] == "true") {
            // set the `env` tag for the test spans
            environment("DD_ENV", "ci")
            // add custom tags based on the module and variant (debug/release, flavors, …)
            environment("DD_TAGS", "test.module:$moduleName")

            // disable other Datadog integrations that could interact with the Java Agent
            environment("DD_INTEGRATIONS_ENABLED", "false")
            // disable JMX integration
            environment("DD_JMX_FETCH_ENABLED", "false")

            jvmArgs("-javaagent:${javaAgentJar.absolutePath}")
        }
    }
}
