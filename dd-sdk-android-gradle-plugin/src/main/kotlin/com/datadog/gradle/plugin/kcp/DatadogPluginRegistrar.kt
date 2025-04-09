package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

/**
 * Note that [ComponentRegistrar] is only deprecated in Kotlin 1.x and will not be deprecated in
 * Kotlin 2.0, as clarified in the following issue comment:
 * https://youtrack.jetbrains.com/issue/KT-52665/Deprecate-ComponentRegistrar#focus=Comments-27-7999959.0-0
 *
 * The reason for using [ComponentRegistrar] instead of [CompilerPluginRegistrar] is that the latter
 * does not provide a way to define the order of plugins.
 * This ordering is crucial because we need the Datadog plugin to execute before the Compose
 * Compiler Plugin, ensuring that the injected code is properly instrumented by the Compose
 * Compiler Plugin.
 */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(ComponentRegistrar::class)
internal class DatadogPluginRegistrar(
    private val overrideInstrumentationMode: InstrumentationMode? = null
) : ComponentRegistrar {
    // Supports Kotlin 2.x compiler
    override val supportsK2 = true

    @OptIn(FirIncompatiblePluginAPI::class)
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        val messageCollector =
            configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        val internalCompilerConfiguration =
            overrideInstrumentationMode ?: resolveConfiguration(configuration)
        project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
            .registerExtension(
                ComposeNavHostExtension(messageCollector, internalCompilerConfiguration),
                LoadingOrder.FIRST,
                project
            )
        project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
            .registerExtension(
                ComposeTagExtension(messageCollector, internalCompilerConfiguration),
                LoadingOrder.FIRST,
                project
            )
    }

    private fun resolveConfiguration(configuration: CompilerConfiguration): InstrumentationMode {
        return resolveOptionValue(configuration.get(CONFIG_INSTRUMENTATION_MODE))
    }

    private fun resolveOptionValue(value: String?): InstrumentationMode {
        return value?.let { InstrumentationMode.from(it) } ?: InstrumentationMode.DISABLE
    }

    companion object {
        private const val OPTION_KEY_INSTRUMENTATION_MODE = "INSTRUMENTATION_MODE"
        val CONFIG_INSTRUMENTATION_MODE = CompilerConfigurationKey.create<String>(OPTION_KEY_INSTRUMENTATION_MODE)
    }
}
