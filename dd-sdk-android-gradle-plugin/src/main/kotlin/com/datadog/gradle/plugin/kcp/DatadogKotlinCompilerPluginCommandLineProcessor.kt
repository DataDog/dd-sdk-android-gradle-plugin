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
            optionName = INSTRUMENTATION_MODE,
            valueDescription = STRING_VALUE_DESCRIPTION,
            description = INSTRUMENTATION_MODE_DESCRIPTION,
            required = false
        )
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        if (option.optionName == INSTRUMENTATION_MODE) {
            configuration.put(
                DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE,
                value
            )
        }
    }

    companion object {
        private const val STRING_VALUE_DESCRIPTION = "<string>"
        internal const val KOTLIN_COMPILER_PLUGIN_ID = "com.datadoghq.kotlin.compiler"
        internal const val INSTRUMENTATION_MODE = "INSTRUMENTATION_MODE"
        private const val INSTRUMENTATION_MODE_DESCRIPTION = "Datadog compiler plugin instrumentation mode option."
    }
}
