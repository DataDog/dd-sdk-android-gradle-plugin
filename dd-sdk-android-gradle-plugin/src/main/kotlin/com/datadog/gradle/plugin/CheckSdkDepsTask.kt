/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import com.datadog.gradle.plugin.internal.SdkSearchResult
import org.gradle.api.DefaultTask
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
    internal abstract val sdkSearchResult: Property<SdkSearchResult>

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
        when (val result = sdkSearchResult.get()) {
            is SdkSearchResult.Present -> Unit
            is SdkSearchResult.Unresolved -> logger.warn(
                CANNOT_RESOLVE_CONFIGURATION_MESSAGE.format(Locale.US, configurationName.get(), result.message)
            )
            is SdkSearchResult.Absent -> {
                val checkLevel = sdkCheckLevel.get()
                when (checkLevel) {
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
                                " value=$checkLevel is not handled"
                        )
                    }
                }
            }
        }
    }

    internal companion object {
        internal const val MISSING_DD_SDK_MESSAGE = "Following application variant doesn't" +
            " have Datadog SDK included: %s"
        internal const val CANNOT_RESOLVE_CONFIGURATION_MESSAGE = "Couldn't resolve configuration %s:\n%s"
    }
}
