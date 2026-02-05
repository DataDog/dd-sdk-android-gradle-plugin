/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/**
 * Abstract base test class for testing ComposeNavHost IR extension transformers.
 *
 * Provides common mock setup for Kotlin compiler plugin context and IR symbols
 * required for testing NavHost-related IR transformations.
 */
@OptIn(FirIncompatiblePluginAPI::class, UnsafeDuringIrConstructionAPI::class)
@ExtendWith(MockitoExtension::class)
abstract class ComposeNavHostExtensionTest {

    @Mock
    lateinit var mockMessageCollector: MessageCollector

    @Mock
    lateinit var mockModuleFragment: IrModuleFragment

    @Mock
    lateinit var mockPluginContext: IrPluginContext

    @Mock
    private lateinit var mockIrSimpleFunctionSymbol: IrSimpleFunctionSymbol

    @Mock
    private lateinit var mockClassSymbol: IrClassSymbol

    @BeforeEach
    fun setUp() {
        // Deep mock in PluginContext to make `initReference` pass for each transformer
        whenever(mockPluginContext.referenceFunctions(any<CallableId>())) doReturn listOf(mockIrSimpleFunctionSymbol)
        whenever(mockPluginContext.referenceClass(any<ClassId>())) doReturn mockClassSymbol
    }

    /**
     * Verifies that the transformer is properly accepted by the extension.
     * @param annotationModeEnabled whether annotation-based instrumentation mode is enabled
     */
    abstract fun verifyTransformerAccepted(annotationModeEnabled: Boolean)
}
