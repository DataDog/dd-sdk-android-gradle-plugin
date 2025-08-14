package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * The extension registers [ComposeTagTransformer21] into the plugin.
 */
@FirIncompatiblePluginAPI
class ComposeTagExtension21(
    private val messageCollector: MessageCollector,
    private val annotationModeEnabled: Boolean
) :
    IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        registerTagTransformer(
            pluginContext = pluginContext,
            moduleFragment = moduleFragment,
            annotationModeEnabled = annotationModeEnabled
        )
    }

    private fun registerTagTransformer(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        annotationModeEnabled: Boolean
    ) {
        ComposeTagTransformer21(
            messageCollector = messageCollector,
            pluginContext = pluginContext,
            annotationModeEnabled = annotationModeEnabled
        ).apply {
            if (initReferences()) {
                moduleFragment.accept(this, null)
            } else {
                messageCollector.warnDependenciesError()
            }
        }
    }
}
