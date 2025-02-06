package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

// TODO RUM-8583: Implement Compose semantics tag instrumentation.
@Suppress("UnusedPrivateProperty")
internal class ComposeTagTransformer(
    private val messageCollector: MessageCollector,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext()
