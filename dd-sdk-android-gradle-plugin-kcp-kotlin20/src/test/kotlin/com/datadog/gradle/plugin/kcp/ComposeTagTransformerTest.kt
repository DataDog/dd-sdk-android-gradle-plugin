/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class ComposeTagTransformerTest {

    @Mock
    private lateinit var mockPluginContext: IrPluginContext

    @Mock
    private lateinit var mockMessageCollector: MessageCollector

    @Mock
    private lateinit var mockPluginContextUtils: PluginContextUtils

    @Mock
    private lateinit var mockDatadogModifierFunctionSymbol: IrSimpleFunctionSymbol

    @Mock
    private lateinit var mockModifierClassSymbol: IrClassSymbol

    @Mock
    private lateinit var mockThenFunctionSymbol: IrSimpleFunctionSymbol

    @Mock
    private lateinit var mockModifierCompanionClassSymbol: IrClassSymbol

    private lateinit var testedComposeTagTransformer: ComposeTagTransformer

    @BeforeEach
    fun `set up`() {
        testedComposeTagTransformer = ComposeTagTransformer(
            mockMessageCollector,
            mockPluginContext,
            false,
            mockPluginContextUtils
        )
    }

    @Test
    fun `W found all required references M return true`() {
        // Given
        whenever(mockPluginContextUtils.getDatadogModifierSymbol()) doReturn mockDatadogModifierFunctionSymbol
        whenever(mockPluginContextUtils.getModifierClassSymbol()) doReturn mockModifierClassSymbol
        whenever(mockPluginContextUtils.getModifierThen()) doReturn mockThenFunctionSymbol
        whenever(mockPluginContextUtils.getModifierCompanionClass()) doReturn mockModifierCompanionClassSymbol

        // When
        val result = testedComposeTagTransformer.initReferences()

        assertThat(result).isTrue()
    }

    @Test
    fun `W datadog modifier not found M return false`() {
        // Given
        whenever(mockPluginContextUtils.getDatadogModifierSymbol()) doReturn null
        whenever(mockPluginContextUtils.getModifierClassSymbol()) doReturn mockModifierClassSymbol
        whenever(mockPluginContextUtils.getModifierThen()) doReturn mockThenFunctionSymbol
        whenever(mockPluginContextUtils.getModifierCompanionClass()) doReturn mockModifierCompanionClassSymbol

        // When
        val result = testedComposeTagTransformer.initReferences()

        assertThat(result).isFalse()
    }

    @Test
    fun `W Modifier class not found M return false`() {
        // Given
        whenever(mockPluginContextUtils.getDatadogModifierSymbol()) doReturn mockDatadogModifierFunctionSymbol
        whenever(mockPluginContextUtils.getModifierClassSymbol()) doReturn null
        whenever(mockPluginContextUtils.getModifierThen()) doReturn mockThenFunctionSymbol
        whenever(mockPluginContextUtils.getModifierCompanionClass()) doReturn mockModifierCompanionClassSymbol

        // When
        val result = testedComposeTagTransformer.initReferences()

        assertThat(result).isFalse()
    }

    @Test
    fun `W modifier then() not found M return false`() {
        // Given
        whenever(mockPluginContextUtils.getDatadogModifierSymbol()) doReturn mockDatadogModifierFunctionSymbol
        whenever(mockPluginContextUtils.getModifierClassSymbol()) doReturn mockModifierClassSymbol
        whenever(mockPluginContextUtils.getModifierThen()) doReturn null
        whenever(mockPluginContextUtils.getModifierCompanionClass()) doReturn mockModifierCompanionClassSymbol

        // When
        val result = testedComposeTagTransformer.initReferences()

        assertThat(result).isFalse()
    }

    @Test
    fun `W Modifier Companion class not found M return false`() {
        // Given
        whenever(mockPluginContextUtils.getDatadogModifierSymbol()) doReturn mockDatadogModifierFunctionSymbol
        whenever(mockPluginContextUtils.getModifierClassSymbol()) doReturn mockModifierClassSymbol
        whenever(mockPluginContextUtils.getModifierThen()) doReturn mockThenFunctionSymbol
        whenever(mockPluginContextUtils.getModifierCompanionClass()) doReturn null

        // When
        val result = testedComposeTagTransformer.initReferences()

        assertThat(result).isFalse()
    }
}
