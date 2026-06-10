/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Implementation of [CompilerPluginRegistrar] with Kotlin 2.4.x support.
 *
 * Uses [CompilerPluginRegistrar] instead of [ComponentRegistrar] because
 * [IrGenerationExtension.extensionPointName] was removed in Kotlin 2.4.0.
 * Plugin ordering (Datadog before Compose) is enforced via -Xcompiler-plugin-order,
 * injected by [DatadogKotlinCompilerPluginSupport].
 */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class DatadogPluginRegistrarImpl(
    private val overrideInstrumentationMode: InstrumentationMode? = null
) : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    // Required in Kotlin 2.3.0+ to enable -Xcompiler-plugin-order; must match
    // DatadogKotlinCompilerPluginCommandLineProcessor.pluginId.
    override val pluginId: String = "com.datadoghq.kotlin.compiler"

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector =
            configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE]
        val instrumentationMode =
            overrideInstrumentationMode ?: resolveConfiguration(configuration)

        if (instrumentationMode != InstrumentationMode.DISABLE) {
            IrGenerationExtension.registerExtension(
                ComposeNavHostExtension(
                    messageCollector = messageCollector,
                    annotationModeEnabled = instrumentationMode == InstrumentationMode.ANNOTATION
                )
            )
            IrGenerationExtension.registerExtension(
                ComposeTagExtension(
                    messageCollector = messageCollector,
                    annotationModeEnabled = instrumentationMode == InstrumentationMode.ANNOTATION
                )
            )
        }
    }

    private fun resolveConfiguration(configuration: CompilerConfiguration): InstrumentationMode {
        return configuration[DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE]
            ?.let { InstrumentationMode.from(it) }
            ?: InstrumentationMode.DISABLE
    }
}
