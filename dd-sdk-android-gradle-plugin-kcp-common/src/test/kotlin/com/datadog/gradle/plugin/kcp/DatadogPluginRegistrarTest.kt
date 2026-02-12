/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionsAreaImpl
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@OptIn(FirIncompatiblePluginAPI::class)
@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class DatadogPluginRegistrarTest {

    @Mock
    private lateinit var mockProject: MockProject

    @Mock
    private lateinit var mockExtensionArea: ExtensionsAreaImpl

    @Mock
    private lateinit var mockExtensionPoint: ExtensionPoint<IrGenerationExtension>

    private lateinit var compilerConfiguration: CompilerConfiguration

    @Mock
    private lateinit var mockMessageCollector: MessageCollector

    @BeforeEach
    fun `set up`() {
        compilerConfiguration = CompilerConfiguration()
        compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, mockMessageCollector)
        whenever(mockProject.extensionArea).thenReturn(mockExtensionArea)
        whenever(
            mockExtensionArea
                .getExtensionPoint(any<ExtensionPointName<IrGenerationExtension>>())
        )
            .thenReturn(mockExtensionPoint)
    }

    @Test
    fun `M register extensions W registerProjectComponents() { mode == AUTO }`() {
        // Given
        val instrumentationMode = InstrumentationMode.AUTO
        compilerConfiguration.put(
            DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE,
            instrumentationMode.name
        )

        val testedRegistrar = StubDatadogPluginRegistrar(
            instrumentationMode = instrumentationMode
        )

        // When
        testedRegistrar.registerProjectComponents(mockProject, compilerConfiguration)

        // Then
        argumentCaptor<IrGenerationExtension> {
            verify(mockExtensionPoint, times(2)).registerExtension(
                capture(),
                eq(LoadingOrder.FIRST),
                eq(mockProject)
            )
        }
    }

    @Test
    fun `M register extensions W registerProjectComponents() { mode == ANNOTATION }`() {
        // Given
        val instrumentationMode = InstrumentationMode.ANNOTATION
        compilerConfiguration.put(
            DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE,
            instrumentationMode.name
        )

        val testedRegistrar = StubDatadogPluginRegistrar(
            instrumentationMode = instrumentationMode
        )

        // When
        testedRegistrar.registerProjectComponents(mockProject, compilerConfiguration)

        // Then
        argumentCaptor<IrGenerationExtension> {
            verify(mockExtensionPoint, times(2)).registerExtension(
                capture(),
                eq(LoadingOrder.FIRST),
                eq(mockProject)
            )
        }
    }

    @Test
    fun `M not register extensions W registerProjectComponents() { mode = DISABLE }`() {
        // Given
        compilerConfiguration.put(
            DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE,
            InstrumentationMode.DISABLE.name
        )
        val testedRegistrar = StubDatadogPluginRegistrar()

        // When
        testedRegistrar.registerProjectComponents(mockProject, compilerConfiguration)

        // Then
        verify(mockExtensionPoint, never()).registerExtension(
            any<IrGenerationExtension>(),
            any<LoadingOrder>(),
            any()
        )
    }

    @Test
    fun `M use override mode W registerProjectComponents() { overrideInstrumentationMode provided }`() {
        // Given
        val overrideMode = InstrumentationMode.ANNOTATION
        compilerConfiguration.put(
            DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE,
            InstrumentationMode.AUTO.name
        )
        val testedRegistrar = StubDatadogPluginRegistrar(overrideMode)

        // When
        testedRegistrar.registerProjectComponents(mockProject, compilerConfiguration)

        // Then
        argumentCaptor<IrGenerationExtension> {
            verify(mockExtensionPoint, times(2)).registerExtension(
                capture(),
                any<LoadingOrder>(),
                any()
            )
        }
    }

    @Test
    fun `M support K2 compiler`() {
        // Given
        val testedRegistrar = StubDatadogPluginRegistrar()

        // When / Then
        assertThat(testedRegistrar.supportsK2).isTrue()
    }

    class StubDatadogPluginRegistrar(
        instrumentationMode: InstrumentationMode? = null
    ) : DatadogPluginRegistrar(instrumentationMode) {
        override fun getCompatibleComposeNavHostExtension(
            messageCollector: MessageCollector,
            instrumentationMode: InstrumentationMode
        ): IrGenerationExtension {
            return StubComposeNavHostExtension()
        }

        override fun getCompatibleComposeTagExtension(
            messageCollector: MessageCollector,
            instrumentationMode: InstrumentationMode
        ): IrGenerationExtension {
            return StubComposeTagExtension()
        }
    }

    private class StubComposeNavHostExtension : IrGenerationExtension {
        override fun generate(
            moduleFragment: IrModuleFragment,
            pluginContext: IrPluginContext
        ) {
            // do nothing
        }
    }

    private class StubComposeTagExtension : IrGenerationExtension {
        override fun generate(
            moduleFragment: IrModuleFragment,
            pluginContext: IrPluginContext
        ) {
            // do nothing
        }
    }
}
