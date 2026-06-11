/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.compilermeta

import com.datadog.gradle.config.taskConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool

class CompilerMetaPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val apiDir = target.layout.projectDirectory
            .dir("api")
        val compilerMetaFile = apiDir.file("compiler-meta.txt")

        val kotlinCompilations = target.tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>>()
        val generateCompilerMetaTask = target.tasks
            .register<GenerateCompilerMetaTask>(TASK_GEN_COMPILER_METADATA) {
                compiledClassesDirectory.set(
                    kotlinCompilations.named("compileKotlin").flatMap { (it as KotlinCompileTool).destinationDirectory }
                )
                metadataInfoFile.set(compilerMetaFile)
            }

        target.tasks
            .register<CheckCompilerMetaTask>(TASK_CHECK_COMPILER_METADATA) {
                this.metadataInfoFile.set(compilerMetaFile)
                dependsOn(TASK_GEN_COMPILER_METADATA)
            }

        target.taskConfig<KotlinCompilationTask<KotlinJvmCompilerOptions>> {
            finalizedBy(generateCompilerMetaTask)
        }
    }

    companion object {
        const val TASK_GEN_COMPILER_METADATA = "generateCompilerMetadata"
        const val TASK_CHECK_COMPILER_METADATA = "checkCompilerMetadataChanges"
    }
}
