package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * The extension registers [ComposeTagTransformer] into the plugin.
 */
@FirIncompatiblePluginAPI
internal class ComposeTagExtension(
    private val messageCollector: MessageCollector,
    private val internalCompilerConfiguration: InternalCompilerConfiguration
) :
    IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        internalCompilerConfiguration.apply {
            if (recordImages != InstrumentationMode.DISABLE) {
                registerTagTransformer(
                    pluginContext = pluginContext,
                    moduleFragment = moduleFragment,
                    annotationModeEnabled = recordImages == InstrumentationMode.ANNOTATION
                )
            }
        }
    }

    private fun registerTagTransformer(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        annotationModeEnabled: Boolean
    ) {
        val composeTagTransformer = ComposeTagTransformer(
            messageCollector = messageCollector,
            pluginContext = pluginContext,
            annotationModeEnabled = annotationModeEnabled
        )
        if (composeTagTransformer.initReferences()) {
            moduleFragment.accept(composeTagTransformer, null)
        } else {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Datadog Kotlin Compiler Plugin didn't succeed initializing references, abort."
            )
        }
    }
}
