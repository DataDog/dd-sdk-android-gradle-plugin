/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.config.CompilerConfigurationKey

/**
 * Compiler configuration keys shared across all versioned plugin registrar implementations.
 *
 * These keys deliberately live outside [DatadogPluginRegistrar]. That class extends the legacy
 * `ComponentRegistrar`, which Kotlin 2.4.20 removed, so touching a key through its companion forces
 * the JVM to resolve the missing supertype and fail with a `NoClassDefFoundError`. Holding the keys
 * here keeps the option-parsing path free of any compiler-plugin supertype and therefore loadable on
 * every supported Kotlin version.
 */
object DatadogCompilerConfigurationKeys {

    private const val OPTION_KEY_INSTRUMENTATION_MODE = "INSTRUMENTATION_MODE"

    /** Configuration key used to pass the instrumentation mode string to the compiler plugin. */
    val CONFIG_INSTRUMENTATION_MODE: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create(OPTION_KEY_INSTRUMENTATION_MODE)
}
