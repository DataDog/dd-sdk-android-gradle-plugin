/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Implementation of [DatadogPluginRegistrar] with Kotlin 2.2.x support.
 */
@OptIn(ExperimentalCompilerApi::class, FirIncompatiblePluginAPI::class)
@AutoService(ComponentRegistrar::class)
class DatadogPluginRegistrarImpl(overrideInstrumentationMode: InstrumentationMode? = null) :
    DatadogPluginRegistrar(overrideInstrumentationMode) {
    override fun getCompatibleComposeNavHostExtension(
        messageCollector: MessageCollector,
        instrumentationMode: InstrumentationMode
    ): IrGenerationExtension {
        return ComposeNavHostExtension(
            messageCollector = messageCollector,
            annotationModeEnabled = instrumentationMode == InstrumentationMode.ANNOTATION
        )
    }

    override fun getCompatibleComposeTagExtension(
        messageCollector: MessageCollector,
        instrumentationMode: InstrumentationMode
    ): IrGenerationExtension {
        return ComposeTagExtension(
            messageCollector = messageCollector,
            annotationModeEnabled = instrumentationMode == InstrumentationMode.ANNOTATION
        )
    }
}
