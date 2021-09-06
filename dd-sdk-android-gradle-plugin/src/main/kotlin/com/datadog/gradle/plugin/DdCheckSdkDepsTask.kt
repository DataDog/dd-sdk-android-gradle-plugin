package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import java.util.LinkedList
import java.util.Queue
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task to check the Datadog SDK throughout the variant dependencies.
 */
abstract class DdCheckSdkDepsTask :
    DefaultTask() {

    /**
     * The sdkCheckLevel: NONE, WARN, FAIL.
     */
    @get:Input
    abstract val sdkCheckLevel: Property<SdkCheckLevel>

    /**
     * The current variant configuration.
     */
    @get:Input
    abstract val configuration: Property<Configuration>

    /**
     * The variant name.
     */
    @get:Input
    abstract val variantName: Property<String>

    init {
        group = "datadog"
        description = "Checks for the Datadog SDK into your variant dependencies."
        // ignore the outputs when evaluating if this task is up-to-date
        outputs.upToDateWhen { true }
    }

    /**
     * Checks if the Datadog SDK is present in the variant dependencies.
     */
    @TaskAction
    fun applyTask() {
        val topDependencies = configuration.get().resolvedConfiguration.firstLevelModuleDependencies
        if (!isDatadogDependencyPresent(topDependencies)) {
            when (sdkCheckLevel.get()) {
                SdkCheckLevel.FAIL -> {
                    throw MissingSdkException(
                        MISSING_DD_SDK_MESSAGE.format(variantName.get())
                    )
                }
                SdkCheckLevel.WARN -> {
                    DdAndroidGradlePlugin.LOGGER.warn(
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
        }
    }

    internal fun isDatadogDependencyPresent(dependencies: Set<ResolvedDependency>): Boolean {
        val queue: Queue<ResolvedDependency> = LinkedList(dependencies)
        while (queue.isNotEmpty()) {
            val dep = queue.remove()
            if (dep.moduleGroup == DD_SDK_GROUP &&
                dep.moduleName == DD_SDK_NAME
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
        internal const val DD_SDK_NAME = "dd-sdk-android"
        internal const val DD_SDK_GROUP = "com.datadoghq"
    }
}
