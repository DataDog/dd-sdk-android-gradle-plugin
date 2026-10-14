/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.transdeps

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class TransitiveDependenciesTask : DefaultTask() {

    @get:Input
    abstract val humanReadableSize: Property<Boolean>

    @get:Input
    abstract val sortByName: Property<Boolean>

    // Tracks the runtime artifacts content for up-to-date checks, as their sizes are reported
    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    // Same artifacts as runtimeClasspath, with their component identifiers
    @get:Internal
    abstract val runtimeArtifacts: SetProperty<ResolvedArtifactResult>

    // Classpath normalization ignores jar names, so coordinates are tracked separately
    @get:Input
    abstract val runtimeClasspathCoordinates: ListProperty<String>

    // Only coordinates are needed for the compile classpath, so jars content is not fingerprinted
    @get:Input
    abstract val compileClasspathCoordinates: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "datadog"
        description = "Generate the list of transitive dependencies of the library"
    }

    // region Task

    @TaskAction
    fun applyTask() {
        outputFile.get().asFile.let {
            it.writeText("Dependencies List\n\n")

            listConfigurationDependencies(it)
        }
    }

    // endregion

    // region Internal

    private fun listConfigurationDependencies(outputFile: File) {
        val runtimeArtifacts = runtimeArtifacts.get().map {
            Artifact(it.id.componentIdentifier.coordinates(), it.file.length(), isCompileOnly = false)
        }
        val runtimeCoordinates = runtimeArtifacts.map { it.coordinates }.toSet()
        // a module can contribute several jars (e.g. Gradle variants), so dedupe by coordinates
        // compile-only artifacts are not shipped, so they don't add to the size
        val compileOnlyArtifacts = (compileClasspathCoordinates.get().toSet() - runtimeCoordinates)
            .map { Artifact(it, 0L, isCompileOnly = true) }
        val artifacts = runtimeArtifacts + compileOnlyArtifacts

        val sortedArtifacts = if (sortByName.get()) {
            artifacts.sortedBy { it.coordinates }
        } else {
            artifacts.sortedByDescending { it.size }
        }

        var sum = 0L
        sortedArtifacts.forEach {
            sum += it.size
            outputFile.appendText(getDependencyFileDescription(it))
        }

        outputFile.appendText("\n${TOTAL.padEnd(PADDING)}:${size(sum)}\n\n")
    }

    private fun getDependencyFileDescription(artifact: Artifact): String {
        val suffix = if (artifact.isCompileOnly) " $COMPILE_ONLY_SUFFIX" else ""
        val title = "${artifact.coordinates}$suffix"

        return "${title.padEnd(PADDING)}:${size(artifact.size)}\n"
    }

    @Suppress("MagicNumber")
    private fun size(size: Long): String {
        if (humanReadableSize.get()) {
            val rawSize = when {
                size >= 2 * MB -> "${size / MB} Mb"
                size >= 2 * KB -> "${size / KB} Kb"
                else -> "$size b "
            }
            return rawSize.padStart(8)
        } else {
            return "$size b ".padStart(16)
        }
    }

    private class Artifact(
        val coordinates: String,
        val size: Long,
        val isCompileOnly: Boolean
    )

    // endregion

    companion object {
        private const val PADDING = 80
        private const val COMPILE_ONLY_SUFFIX = "(compileOnly)"
        private const val KB = 1024
        private const val MB = 1024 * 1024

        private const val TOTAL = "Total transitive dependencies size"
    }
}

internal fun ComponentIdentifier.coordinates(): String {
    return if (this is ModuleComponentIdentifier) "$group:$module:$version" else displayName
}
