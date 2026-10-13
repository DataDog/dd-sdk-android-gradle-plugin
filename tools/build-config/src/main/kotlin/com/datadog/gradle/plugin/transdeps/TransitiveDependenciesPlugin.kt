/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.transdeps

import com.datadog.gradle.config.taskConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class TransitiveDependenciesPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val externalRuntimeArtifacts = target.externalArtifacts("runtimeClasspath")
        val externalCompileArtifacts = target.externalArtifacts("compileClasspath")

        val listTransitiveDependenciesTask =
            target.tasks.register<TransitiveDependenciesTask>(TASK_GEN_TRANSITIVE_DEPS) {
                humanReadableSize.value(true)
                sortByName.value(true)
                runtimeClasspath.from(externalRuntimeArtifacts.map { it.artifactFiles })
                runtimeArtifacts.set(externalRuntimeArtifacts.flatMap { it.resolvedArtifacts })
                runtimeClasspathCoordinates.set(externalRuntimeArtifacts.coordinates())
                compileClasspathCoordinates.set(externalCompileArtifacts.coordinates())
                outputFile.set(target.layout.projectDirectory.file(FILE_NAME))
            }

        target.tasks.register<CheckTransitiveDependenciesTask>(TASK_CHECK_TRANSITIVE_DEPS) {
            dependenciesFile.set(listTransitiveDependenciesTask.flatMap { it.outputFile })
        }

        target.taskConfig<KotlinJvmCompile> {
            finalizedBy(listTransitiveDependenciesTask)
        }
    }

    private fun Project.externalArtifacts(configurationName: String): Provider<ArtifactCollection> {
        return configurations.named(configurationName).map { configuration ->
            check(configuration.isCanBeResolved) { "$configuration cannot be resolved" }

            configuration.incoming
                .artifactView {
                    componentFilter {
                        it !is ProjectComponentIdentifier && it.displayName != "Gradle API"
                    }
                }
                .artifacts
        }
    }

    private fun Provider<ArtifactCollection>.coordinates(): Provider<List<String>> {
        return flatMap { it.resolvedArtifacts }.map { artifacts ->
            artifacts.map { it.id.componentIdentifier.coordinates() }
        }
    }

    companion object {

        const val TASK_GEN_TRANSITIVE_DEPS = "listTransitiveDependencies"
        const val TASK_CHECK_TRANSITIVE_DEPS = "checkTransitiveDependenciesList"
        const val FILE_NAME = "transitiveDependencies"
    }
}
