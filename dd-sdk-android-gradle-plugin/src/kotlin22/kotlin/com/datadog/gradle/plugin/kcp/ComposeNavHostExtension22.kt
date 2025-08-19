package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * The extension registers [ComposeNavHostTransformer22] into the plugin.
 *
 * Internal use only.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
@FirIncompatiblePluginAPI
class ComposeNavHostExtension22(
    private val messageCollector: MessageCollector,
    private val annotationModeEnabled: Boolean
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        registerNavHostTransformer(
            pluginContext = pluginContext,
            moduleFragment = moduleFragment,
            annotationModeEnabled = annotationModeEnabled
        )
    }

    private fun registerNavHostTransformer(
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
        annotationModeEnabled: Boolean
    ) {
        ComposeNavHostTransformer22(
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
