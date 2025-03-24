package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * The extension registers [ComposeNavHostTransformer] into the plugin.
 */
@FirIncompatiblePluginAPI
@Suppress("UnusedParameter")
internal class ComposeNavHostExtension(
    private val messageCollector: MessageCollector,
    private val internalCompilerConfiguration: InternalCompilerConfiguration
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        internalCompilerConfiguration.apply {
            if (trackViews != InstrumentationMode.DISABLE) {
                registerNavHostTransformer(
                    pluginContext = pluginContext,
                    moduleFragment = moduleFragment,
                    annotationModeEnabled = trackViews == InstrumentationMode.ANNOTATION
                )
            }
        }
    }

    private fun registerNavHostTransformer(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        annotationModeEnabled: Boolean
    ) {
        val composeNavHostTransformer =
            ComposeNavHostTransformer(
                messageCollector = messageCollector,
                pluginContext = pluginContext,
                annotationModeEnabled = annotationModeEnabled
            )
        if (composeNavHostTransformer.initReferences()) {
            moduleFragment.accept(composeNavHostTransformer, null)
        } else {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Datadog Kotlin Compiler Plugin didn't succeed initializing references, abort. " +
                    "Have you added dd-sdk-android-compose library to the dependencies?"
            )
        }
    }
}
