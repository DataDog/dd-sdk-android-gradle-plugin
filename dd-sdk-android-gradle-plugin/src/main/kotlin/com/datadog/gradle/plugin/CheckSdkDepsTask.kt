/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.slf4j.LoggerFactory
import java.util.LinkedList
import java.util.Queue

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
    internal var isLastRunSuccessful: Boolean = true

    init {
        group = DdAndroidGradlePlugin.DATADOG_TASK_GROUP
        description = "Checks for the Datadog SDK into your variant dependencies."
        outputs.upToDateWhen { it is CheckSdkDepsTask && it.isLastRunSuccessful }
    }

    /**
     * Checks if the Datadog SDK is present in the variant dependencies.
     */
    @Suppress("ThrowsCount")
    @TaskAction
    fun applyTask() {
        val configuration =
            project.configurations.firstOrNull { it.name == configurationName.get() }

        if (configuration == null) {
            LOGGER.info(
                CANNOT_FIND_CONFIGURATION_MESSAGE.format(
                    configurationName.get(),
                    variantName.get()
                )
            )
            return
        }

        val resolvedConfiguration = configuration.resolvedConfiguration

        if (resolvedConfiguration.hasError()) {
            val error = try {
                resolvedConfiguration.rethrowFailure()
                throw IllegalStateException(
                    "Expected to throw" +
                        " ${ResolveException::class.qualifiedName}, but this didn't happen"
                )
            } catch (re: ResolveException) {
                re
            }
            LOGGER.warn("Couldn't resolve configuration ${configurationName.get()}", error)
            return
        }

        val topDependencies = resolvedConfiguration.firstLevelModuleDependencies
        if (!isDatadogDependencyPresent(topDependencies)) {
            isLastRunSuccessful = false
            when (sdkCheckLevel.get()) {
                SdkCheckLevel.FAIL -> {
                    throw MissingSdkException(
                        MISSING_DD_SDK_MESSAGE.format(variantName.get())
                    )
                }
                SdkCheckLevel.WARN -> {
                    LOGGER.warn(
                        MISSING_DD_SDK_MESSAGE.format(
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
        } else {
            isLastRunSuccessful = true
        }
    }

    internal fun isDatadogDependencyPresent(dependencies: Set<ResolvedDependency>): Boolean {
        val queue: Queue<ResolvedDependency> = LinkedList(dependencies)
        while (queue.isNotEmpty()) {
            val dep = queue.remove()
            if (dep.moduleGroup == DD_SDK_GROUP &&
                (dep.moduleName == DD_SDK_V1_NAME || dep.moduleName == DD_SDK_V2_CORE_NAME)
            ) {
                return true
            }
            queue.addAll(dep.children)
        }
        return false
    }

    companion object {
        internal const val MISSING_DD_SDK_MESSAGE = "Following application variant doesn't" +
            " have Datadog SDK included: %s"
        internal const val CANNOT_FIND_CONFIGURATION_MESSAGE = "Cannot find configuration %s for" +
            " the variant %s in the configurations list, please" +
            " report the issue at" +
            " https://github.com/DataDog/dd-sdk-android-gradle-plugin/issues"
        internal const val DD_SDK_V1_NAME = "dd-sdk-android"
        internal const val DD_SDK_V2_CORE_NAME = "dd-sdk-android-core"
        internal const val DD_SDK_GROUP = "com.datadoghq"

        internal val LOGGER = LoggerFactory.getLogger("DdCheckSdkDepsTask")
    }
}
