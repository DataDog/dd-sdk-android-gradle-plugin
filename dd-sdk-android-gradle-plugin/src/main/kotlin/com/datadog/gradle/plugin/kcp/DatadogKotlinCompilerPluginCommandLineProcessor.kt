package com.datadog.gradle.plugin.kcp

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
internal class DatadogKotlinCompilerPluginCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = KOTLIN_COMPILER_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = KotlinCompilerPluginOptions.TRACK_VIEWS,
            valueDescription = STRING_VALUE_DESCRIPTION,
            description = TRACK_VIEWS_DESCRIPTION,
            required = false
        ),
        CliOption(
            optionName = KotlinCompilerPluginOptions.TRACK_ACTIONS,
            valueDescription = STRING_VALUE_DESCRIPTION,
            description = TRACK_ACTIONS_DESCRIPTION,
            required = false
        ),
        CliOption(
            optionName = KotlinCompilerPluginOptions.RECORD_IMAGES,
            valueDescription = STRING_VALUE_DESCRIPTION,
            description = RECORD_IMAGES_DESCRIPTION,
            required = false
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            KotlinCompilerPluginOptions.TRACK_VIEWS -> configuration.put(
                DatadogPluginRegistrar.CONFIG_TRACK_VIEWS,
                value
            )

            KotlinCompilerPluginOptions.TRACK_ACTIONS -> configuration.put(
                DatadogPluginRegistrar.CONFIG_TRACK_ACTIONS,
                value
            )

            KotlinCompilerPluginOptions.RECORD_IMAGES -> configuration.put(
                DatadogPluginRegistrar.CONFIG_RECORD_IMAGES,
                value
            )
        }
    }

    companion object {
        private const val STRING_VALUE_DESCRIPTION = "<string>"
        private const val TRACK_VIEWS_DESCRIPTION = "Track Views"
        private const val TRACK_ACTIONS_DESCRIPTION = "Track Actions"
        private const val RECORD_IMAGES_DESCRIPTION = "Record Images"
        internal const val KOTLIN_COMPILER_PLUGIN_ID = "com.datadoghq.kotlin.compiler"
    }
}
