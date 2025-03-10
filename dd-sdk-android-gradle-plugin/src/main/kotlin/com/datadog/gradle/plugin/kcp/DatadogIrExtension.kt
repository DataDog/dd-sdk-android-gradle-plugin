package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

/**
 * The extension registers all the visitors that need to explore the code being compiled.
 */
@FirIncompatiblePluginAPI
internal class DatadogIrExtension(private val messageCollector: MessageCollector) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val composeTagTransformer = ComposeTagTransformer(messageCollector, pluginContext)
        val composeNavHostTransformer = ComposeNavHostTransformer(messageCollector, pluginContext)
        if (composeTagTransformer.initReferences()) {
            moduleFragment.accept(composeTagTransformer, null)
        } else {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Datadog Kotlin Compiler Plugin didn't succeed initializing references, abort."
            )
        }
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
