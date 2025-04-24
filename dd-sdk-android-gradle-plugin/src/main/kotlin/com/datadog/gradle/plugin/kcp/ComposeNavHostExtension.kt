@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * The extension registers [ComposeNavHostTransformer] into the plugin.
 */
@FirIncompatiblePluginAPI
internal class ComposeNavHostExtension(
    private val messageCollector: MessageCollector,
    private val internalInstrumentationMode: InstrumentationMode
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (internalInstrumentationMode != InstrumentationMode.DISABLE) {
            registerNavHostTransformer(
                pluginContext = pluginContext,
                moduleFragment = moduleFragment,
                annotationModeEnabled = internalInstrumentationMode == InstrumentationMode.ANNOTATION
            )
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
            messageCollector.warnDependenciesError()
        }
    }
}
