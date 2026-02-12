/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * The extension registers [ComposeNavHostTransformer] into the plugin.
 *
 * Internal use only.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
@FirIncompatiblePluginAPI
class ComposeNavHostExtension(
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
        ComposeNavHostTransformer(
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
