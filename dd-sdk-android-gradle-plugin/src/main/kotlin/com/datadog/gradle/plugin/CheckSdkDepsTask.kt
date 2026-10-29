/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import com.datadog.gradle.plugin.internal.SdkDependencyGraph
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.util.Locale

/**
 * A Gradle task to check the Datadog SDK throughout the variant dependencies.
 */
abstract class CheckSdkDepsTask : DefaultTask() {

    /**
     * The sdkCheckLevel: NONE, WARN, FAIL.
     */
    @get:Input
    abstract val sdkCheckLevel: Property<SdkCheckLevel>

    /**
     * The current variant configuration name.
     */
    @get:Input
    abstract val configurationName: Property<String>

    /**
     * The variant name.
     */
    @get:Input
    abstract val variantName: Property<String>

    @get:Internal
    internal val dependencyGraph: Property<SdkDependencyGraph> =
        project.objects.property(SdkDependencyGraph::class.java)

    init {
        group = DdAndroidGradlePlugin.DATADOG_TASK_GROUP
        description = "Checks for the Datadog SDK into your variant dependencies."
    }

    /**
     * Checks if the Datadog SDK is present in the variant dependencies.
     */
    @Suppress("ThrowsCount")
    @TaskAction
    fun applyTask() {
        val dependencies = when (val graph = dependencyGraph.orNull) {
            is SdkDependencyGraph.Resolved -> resolveDependencies(graph.root)
            is SdkDependencyGraph.Failed -> {
                logger.warn("Couldn't resolve configuration ${configurationName.get()}", graph.failure)
                null
            }
            null -> {
                logger.info(
                    CANNOT_FIND_CONFIGURATION_MESSAGE.format(
                        Locale.US,
                        configurationName.get(),
                        variantName.get()
                    )
                )
                null
            }
        } ?: return

        if (!isDatadogDependencyPresent(dependencies)) {
            when (sdkCheckLevel.get()) {
                SdkCheckLevel.FAIL -> {
                    throw MissingSdkException(
                        MISSING_DD_SDK_MESSAGE.format(Locale.US, variantName.get())
                    )
                }
                SdkCheckLevel.WARN -> {
                    logger.warn(
                        MISSING_DD_SDK_MESSAGE.format(
                            Locale.US,
                            variantName.get()
                        )
                    )
                }
                else -> {
                    throw IllegalArgumentException(
                        "This should never happen," +
                            " value=$sdkCheckLevel is not handled"
                    )
                }
            }
        }
    }

    private fun resolveDependencies(root: ResolvedComponentResult): Set<ResolvedComponentResult>? {
        val queue = ArrayDeque(root.dependencies)
        val visited = mutableSetOf(root.id)
        val components = mutableSetOf<ResolvedComponentResult>()
        while (queue.isNotEmpty()) {
            when (val dependency = queue.removeFirst()) {
                is UnresolvedDependencyResult -> {
                    logger.warn("Couldn't resolve configuration ${configurationName.get()}", dependency.failure)
                    return null
                }
                is ResolvedDependencyResult -> {
                    if (visited.add(dependency.selected.id)) {
                        components.add(dependency.selected)
                        queue.addAll(dependency.selected.dependencies)
                    }
                }
            }
        }
        return components
    }

    private fun isDatadogDependencyPresent(dependencies: Set<ResolvedComponentResult>): Boolean =
        dependencies.any { dependency ->
            val module = dependency.moduleVersion
            module?.group == DD_SDK_GROUP &&
                (module.name == DD_SDK_V1_NAME || module.name == DD_SDK_V2_CORE_NAME)
        }

    internal companion object {
        internal const val MISSING_DD_SDK_MESSAGE = "Following application variant doesn't" +
            " have Datadog SDK included: %s"
        internal const val CANNOT_FIND_CONFIGURATION_MESSAGE = "Cannot find configuration %s for" +
            " the variant %s in the configurations list, please" +
            " report the issue at" +
            " https://github.com/DataDog/dd-sdk-android-gradle-plugin/issues"
        internal const val DD_SDK_V1_NAME = "dd-sdk-android"
        internal const val DD_SDK_V2_CORE_NAME = "dd-sdk-android-core"
        internal const val DD_SDK_GROUP = "com.datadoghq"
    }
}
