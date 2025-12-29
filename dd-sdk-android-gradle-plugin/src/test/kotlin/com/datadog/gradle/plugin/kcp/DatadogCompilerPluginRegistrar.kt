/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("Deprecation")

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion

/**
 * Test-only implementation of [CompilerPluginRegistrar] for use with kctfork 0.12.0+.
 * This is needed because kctfork 0.12.0 removed the `componentRegistrars` API and only
 * supports `compilerPluginRegistrars` which requires [CompilerPluginRegistrar].
 *
 * This class provides the same functionality as [DatadogPluginRegistrar] but through
 * the newer API.
 */
@OptIn(ExperimentalCompilerApi::class)
internal class DatadogCompilerPluginRegistrar(
    private val overrideInstrumentationMode: InstrumentationMode? = null
) : CompilerPluginRegistrar() {

    override val supportsK2 = true

    @OptIn(FirIncompatiblePluginAPI::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector =
            configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val internalCompilerConfiguration =
            overrideInstrumentationMode ?: resolveConfiguration(configuration)

        if (internalCompilerConfiguration != InstrumentationMode.DISABLE) {
            IrGenerationExtension.registerExtension(
                getCompatibleComposeNavHostExtension(
                    messageCollector = messageCollector,
                    internalCompilerConfiguration = internalCompilerConfiguration
                )
            )
            IrGenerationExtension.registerExtension(
                getCompatibleComposeTagExtension(
                    messageCollector = messageCollector,
                    internalCompilerConfiguration = internalCompilerConfiguration
                )
            )
        }
    }

    @OptIn(FirIncompatiblePluginAPI::class)
    private fun getCompatibleComposeNavHostExtension(
        messageCollector: MessageCollector,
        internalCompilerConfiguration: InstrumentationMode
    ): IrGenerationExtension {
        return when (KotlinVersion.from(KotlinCompilerVersion.getVersion())) {
            KotlinVersion.KOTLIN22 -> ComposeNavHostExtension22(
                messageCollector = messageCollector,
                annotationModeEnabled = internalCompilerConfiguration == InstrumentationMode.ANNOTATION
            )

            KotlinVersion.KOTLIN21 -> ComposeNavHostExtension21(
                messageCollector = messageCollector,
                annotationModeEnabled = internalCompilerConfiguration == InstrumentationMode.ANNOTATION
            )

            KotlinVersion.KOTLIN20 -> ComposeNavHostExtension20(
                messageCollector = messageCollector,
                annotationModeEnabled = internalCompilerConfiguration == InstrumentationMode.ANNOTATION
            )
        }
    }

    @OptIn(FirIncompatiblePluginAPI::class)
    private fun getCompatibleComposeTagExtension(
        messageCollector: MessageCollector,
        internalCompilerConfiguration: InstrumentationMode
    ): IrGenerationExtension {
        return when (KotlinVersion.from(KotlinCompilerVersion.getVersion())) {
            KotlinVersion.KOTLIN22 -> ComposeTagExtension22(
                messageCollector = messageCollector,
                annotationModeEnabled = internalCompilerConfiguration == InstrumentationMode.ANNOTATION
            )

            KotlinVersion.KOTLIN21 -> ComposeTagExtension21(
                messageCollector = messageCollector,
                annotationModeEnabled = internalCompilerConfiguration == InstrumentationMode.ANNOTATION
            )

            KotlinVersion.KOTLIN20 -> ComposeTagExtension20(
                messageCollector = messageCollector,
                annotationModeEnabled = internalCompilerConfiguration == InstrumentationMode.ANNOTATION
            )
        }
    }

    private fun resolveConfiguration(configuration: CompilerConfiguration): InstrumentationMode {
        return resolveOptionValue(configuration.get(DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE))
    }

    private fun resolveOptionValue(value: String?): InstrumentationMode {
        return value?.let { InstrumentationMode.from(it) } ?: InstrumentationMode.DISABLE
    }
}
