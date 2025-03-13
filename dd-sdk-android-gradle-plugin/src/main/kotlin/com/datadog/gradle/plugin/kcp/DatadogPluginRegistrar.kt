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
    private val overrideCompilerConfiguration: InternalCompilerConfiguration? = null
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
            overrideCompilerConfiguration ?: resolveConfiguration(configuration)
        project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
            .registerExtension(
                ComposeNavHostExtension(messageCollector, internalCompilerConfiguration),
                LoadingOrder.FIRST,
                project
            )
        project.extensionArea.getExtensionPoint(IrGenerationExtension.extensionPointName)
            .registerExtension(
                ComposeTagExtension(messageCollector, internalCompilerConfiguration),
                LoadingOrder.LAST,
                project
            )
    }

    private fun resolveConfiguration(configuration: CompilerConfiguration): InternalCompilerConfiguration {
        val trackViewsMode = resolveOptionValue(configuration.get(CONFIG_TRACK_VIEWS))
        val trackActionsMode = resolveOptionValue(configuration.get(CONFIG_TRACK_ACTIONS))
        val recordImagesMode = resolveOptionValue(configuration.get(CONFIG_RECORD_IMAGES))

        return InternalCompilerConfiguration(
            trackViews = trackViewsMode,
            trackActions = trackActionsMode,
            recordImages = recordImagesMode
        )
    }

    private fun resolveOptionValue(value: String?): InstrumentationMode {
        return value?.let { InstrumentationMode.from(it) } ?: InstrumentationMode.DISABLE
    }

    companion object {
        private const val OPTION_KEY_TRACK_VIEWS = "TRACK_VIEWS"
        private const val OPTION_KEY_TRACK_ACTIONS = "TRACK_ACTIONS"
        private const val OPTION_KEY_RECORD_IMAGES = "RECORD_IMAGES"
        val CONFIG_TRACK_VIEWS =
            CompilerConfigurationKey.create<String>(OPTION_KEY_TRACK_VIEWS)
        val CONFIG_TRACK_ACTIONS =
            CompilerConfigurationKey.create<String>(OPTION_KEY_TRACK_ACTIONS)
        val CONFIG_RECORD_IMAGES =
            CompilerConfigurationKey.create<String>(OPTION_KEY_RECORD_IMAGES)
    }
}
