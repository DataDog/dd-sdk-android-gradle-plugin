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
import org.jetbrains.kotlin.config.MessageCollectorAccess

/**
 * Implementation of [CompilerPluginRegistrar] with Kotlin 2.4.x support.
 *
 * Uses [CompilerPluginRegistrar] instead of the legacy `ComponentRegistrar` because
 * `IrGenerationExtension.extensionPointName` was removed in Kotlin 2.4.0, and `ComponentRegistrar`
 * itself was removed in Kotlin 2.4.20. Plugin ordering (Datadog before Compose) is enforced via
 * -Xcompiler-plugin-order, injected by `DatadogKotlinCompilerPluginSupport`.
 *
 * Because the legacy class is gone, nothing on this code path may touch [DatadogPluginRegistrar]:
 * configuration keys are read from [DatadogCompilerConfigurationKeys] instead.
 */
@OptIn(ExperimentalCompilerApi::class, MessageCollectorAccess::class)
@AutoService(CompilerPluginRegistrar::class)
class DatadogPluginRegistrarImpl(
    private val overrideInstrumentationMode: InstrumentationMode? = null
) : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    // Required in Kotlin 2.3.0+ to enable -Xcompiler-plugin-order; must match
    // DatadogKotlinCompilerPluginCommandLineProcessor.pluginId.
    override val pluginId: String = "com.datadoghq.kotlin.compiler"

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Kotlin 2.4.20 gates MESSAGE_COLLECTOR_KEY behind the MessageCollectorAccess opt-in marker.
        // We keep reading the collector directly because the shared IR extensions in kcp-common are
        // built around a MessageCollector instance, which CompilerConfiguration.report cannot supply.
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
        return configuration[DatadogCompilerConfigurationKeys.CONFIG_INSTRUMENTATION_MODE]
            ?.let { InstrumentationMode.from(it) }
            ?: InstrumentationMode.DISABLE
    }
}
