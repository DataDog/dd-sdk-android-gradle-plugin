/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.config

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.kotlinConfig() {
    taskConfig<KotlinCompile> {
        compilerOptions {
            // TODO RUMM-3257 Target Java 17 bytecode at some point
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}
