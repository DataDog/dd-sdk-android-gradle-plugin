/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
@ForgeConfiguration(Configurator::class)
class DefaultPluginContextUtilsTest {

    @Mock
    private lateinit var mockPluginContext: IrPluginContext

    @Mock
    private lateinit var mockMessageCollector: MessageCollector

    @Mock
    private lateinit var mockModifierClassSymbol: IrClassSymbol

    @Mock
    private lateinit var mockModifierClassOwnerSymbol: IrClass

    @Mock
    private lateinit var mockModifierCompanionClass: IrClass

    @Mock
    private lateinit var mockModifierCompanionClassSymbol: IrClassSymbol

    @Mock
    private lateinit var mockDatadogModifierFunctionSymbol: IrSimpleFunctionSymbol

    @Mock
    private lateinit var mockSimpleFunctionSymbol: IrSimpleFunctionSymbol

    private lateinit var fakeCallableId: CallableId

    private lateinit var pluginContextUtils: DefaultPluginContextUtils

    private val modifierClassId = ClassId(composeUiPackageName, modifierClassRelativeName)

    @BeforeEach
    fun `set up`(forge: Forge) {
        pluginContextUtils = DefaultPluginContextUtils(mockPluginContext, mockMessageCollector)
        fakeCallableId = CallableId(
            packageName = FqName(forge.anAsciiString()),
            className = FqName(forge.anAsciiString()),
            callableName = Name.identifier(forge.anAsciiString()),
            pathToLocal = FqName(forge.anAsciiString())
        )
    }

    @Test
    fun `M return Modifier Companion class symbol W have the dependency`() {
        // Given
        whenever(mockPluginContext.referenceClass(modifierClassId)) doReturn mockModifierClassSymbol
        whenever(mockModifierClassSymbol.owner) doReturn mockModifierClassOwnerSymbol
        whenever(mockModifierCompanionClass.isCompanion) doReturn true
        whenever(mockModifierClassOwnerSymbol.declarations) doReturn listOf(mockModifierCompanionClass).toMutableList()
        whenever(mockModifierCompanionClass.symbol) doReturn mockModifierCompanionClassSymbol

        // When
        val companionClassSymbol = pluginContextUtils.getModifierCompanionClass()

        // Then
        assertThat(companionClassSymbol).isEqualTo(mockModifierCompanionClassSymbol)
    }

    @Test
    fun `M return Modifier class symbol W have the dependency`() {
        // Given
        whenever(mockPluginContext.referenceClass(modifierClassId)) doReturn mockModifierClassSymbol

        // When
        val modifierClassSymbol = pluginContextUtils.getModifierClassSymbol()

        // Then
        assertThat(modifierClassSymbol).isEqualTo(mockModifierClassSymbol)
    }

    @Test
    fun `M return datadog modifier symbol W have the dependency`() {
        // Given
        whenever(mockPluginContext.referenceFunctions(fakeCallableId)) doReturn
            listOf(mockDatadogModifierFunctionSymbol)

        // When
        val irSimpleFunctionSymbol = pluginContextUtils.referenceSingleFunction(fakeCallableId)

        // Then
        assertThat(irSimpleFunctionSymbol).isEqualTo(mockDatadogModifierFunctionSymbol)
    }

    @Test
    fun `M return function symbol W call referenceSingleFunction`() {
        // Given
        whenever(mockPluginContext.referenceFunctions(fakeCallableId)) doReturn listOf(mockSimpleFunctionSymbol)

        // When
        val irSimpleFunctionSymbol = pluginContextUtils.referenceSingleFunction(fakeCallableId)

        // Then
        assertThat(irSimpleFunctionSymbol).isEqualTo(mockSimpleFunctionSymbol)
    }

    companion object {
        private val composeUiPackageName = FqName("androidx.compose.ui")
        private val modifierClassRelativeName = Name.identifier("Modifier")
    }
}
