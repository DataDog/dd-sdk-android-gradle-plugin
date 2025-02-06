package com.datadog.gradle.plugin.kcp

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * The Registrar makes the plugin visible to the Kotlin Compiler, and provides the extensions to use.
 */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class DatadogPluginRegistrar : CompilerPluginRegistrar() {

    // Supports Kotlin 2.x compiler
    override val supportsK2 = true

    @OptIn(FirIncompatiblePluginAPI::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector =
            configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        IrGenerationExtension.registerExtension(DatadogIrExtension(messageCollector))
    }
}
