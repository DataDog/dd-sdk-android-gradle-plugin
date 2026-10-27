/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.provider.Provider

internal sealed class SdkSearchResult {
    data object Present : SdkSearchResult()

    data object Absent : SdkSearchResult()

    data class Unresolved(val message: String) : SdkSearchResult()
}

internal object SdkDependencySearch {

    internal const val DD_SDK_GROUP = "com.datadoghq"
    internal const val DD_SDK_V1_NAME = "dd-sdk-android"
    internal const val DD_SDK_V2_CORE_NAME = "dd-sdk-android-core"

    fun search(rootComponent: Provider<ResolvedComponentResult>): SdkSearchResult =
        // Gradle can resolve this provider while storing the cache, before the task can handle failures.
        try {
            search(rootComponent.get())
        } catch (e: ResolveException) {
            SdkSearchResult.Unresolved(e.describe())
        }

    private fun search(root: ResolvedComponentResult): SdkSearchResult {
        val queue = ArrayDeque(root.dependencies)
        val visited = mutableSetOf(root.id)
        var result: SdkSearchResult? = null
        while (result == null && queue.isNotEmpty()) {
            when (val dependency = queue.removeFirst()) {
                is UnresolvedDependencyResult -> result = SdkSearchResult.Unresolved(dependency.failure.describe())
                is ResolvedDependencyResult -> {
                    if (dependency.selected.isDatadogSdk()) {
                        result = SdkSearchResult.Present
                    } else if (visited.add(dependency.selected.id)) {
                        queue.addAll(dependency.selected.dependencies)
                    }
                }
            }
        }
        return result ?: SdkSearchResult.Absent
    }

    private fun ResolvedComponentResult.isDatadogSdk(): Boolean {
        val module = moduleVersion
        return module?.group == DD_SDK_GROUP &&
            (module.name == DD_SDK_V1_NAME || module.name == DD_SDK_V2_CORE_NAME)
    }

    // The top-level message is generic, the actual reason (e.g. a version conflict) is in the causes.
    private fun Throwable.describe(): String =
        generateSequence(this) { it.cause }
            .mapNotNull { it.message }
            .distinct()
            .joinToString(separator = "\n")
}
