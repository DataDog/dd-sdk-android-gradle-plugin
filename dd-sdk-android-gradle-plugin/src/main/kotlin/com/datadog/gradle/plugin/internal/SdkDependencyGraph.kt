/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider

internal sealed class SdkDependencyGraph {
    class Resolved(val root: ResolvedComponentResult) : SdkDependencyGraph()

    class Failed(val failure: ResolveException) : SdkDependencyGraph()

    companion object {
        fun resolve(rootComponent: Provider<ResolvedComponentResult>): SdkDependencyGraph =
            // Gradle can resolve this provider while storing the cache, before the task can handle failures.
            try {
                Resolved(rootComponent.get())
            } catch (e: ResolveException) {
                Failed(e)
            }
    }
}
