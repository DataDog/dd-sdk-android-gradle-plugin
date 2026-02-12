/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.kotlinFqName
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@OptIn(UnsafeDuringIrConstructionAPI::class)
@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock
    private lateinit var mockNavHostControllerClassSymbol: IrClassSymbol

    @Mock
    private lateinit var mockIrFunction: IrFunction

    @Mock
    private lateinit var mockIrPackageFragment: IrPackageFragment

    @Mock
    private lateinit var mockParent: IrPackageFragment

    private lateinit var fakeCallableId: CallableId

    private lateinit var pluginContextUtils: DefaultPluginContextUtils

    private val modifierClassId = ClassId(composeUiPackageName, modifierClassRelativeName)

    /**
     * Sets up the annotation chain for hasAnnotation to return false (no matching annotation).
     */
    private fun setupNoMatchingAnnotation() {
        whenever(mockIrFunction.annotations) doReturn emptyList()
    }

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

    @Test
    fun `M return null and log warning W getModifierClassSymbol and class not found`() {
        // Given
        whenever(mockPluginContext.referenceClass(modifierClassId)) doReturn null

        // When
        val result = pluginContextUtils.getModifierClassSymbol()

        // Then
        assertThat(result).isNull()
        verify(mockMessageCollector).report(
            eq(CompilerMessageSeverity.WARNING),
            eq("androidx/compose/ui/Modifier is not found."),
            isNull()
        )
    }

    @Test
    fun `M return null and log warning W getModifierCompanionClass and class not found`() {
        // Given
        whenever(mockPluginContext.referenceClass(modifierClassId)) doReturn null

        // When
        val result = pluginContextUtils.getModifierCompanionClass()

        // Then
        assertThat(result).isNull()
        verify(mockMessageCollector).report(
            CompilerMessageSeverity.WARNING,
            "Modifier.Companion is not found."
        )
    }

    @Test
    fun `M return null and log warning W getModifierCompanionClass and companion not found`() {
        // Given
        whenever(mockPluginContext.referenceClass(modifierClassId)) doReturn mockModifierClassSymbol
        whenever(mockModifierClassSymbol.owner) doReturn mockModifierClassOwnerSymbol
        whenever(mockModifierClassOwnerSymbol.declarations) doReturn mutableListOf()

        // When
        val result = pluginContextUtils.getModifierCompanionClass()

        // Then
        assertThat(result).isNull()
        verify(mockMessageCollector).report(
            eq(CompilerMessageSeverity.WARNING),
            eq("Modifier.Companion is not found."),
            isNull()
        )
    }

    @Test
    fun `M return null and log error W referenceSingleFunction with no matches and isCritical true`() {
        // Given
        whenever(mockPluginContext.referenceFunctions(fakeCallableId)) doReturn emptyList()

        // When
        val result = pluginContextUtils.referenceSingleFunction(fakeCallableId, isCritical = true)

        // Then
        val argumentCaptor = argumentCaptor<String>()
        assertThat(result).isNull()
        verify(mockMessageCollector).report(
            eq(CompilerMessageSeverity.ERROR),
            argumentCaptor.capture(),
            isNull()
        )
        assertThat(argumentCaptor.firstValue).contains("has none or several references.")
    }

    @Test
    fun `M return null and log strong warning W referenceSingleFunction with no matches and isCritical false`() {
        // Given
        whenever(mockPluginContext.referenceFunctions(fakeCallableId)) doReturn emptyList()

        // When
        val result = pluginContextUtils.referenceSingleFunction(fakeCallableId, isCritical = false)

        // Then
        assertThat(result).isNull()
        val argumentCaptor = argumentCaptor<String>()
        verify(mockMessageCollector).report(
            eq(CompilerMessageSeverity.STRONG_WARNING),
            argumentCaptor.capture(),
            isNull()
        )
        assertThat(argumentCaptor.firstValue).contains("has none or several references.")
    }

    @Test
    fun `M return null and log error W referenceSingleFunction with multiple matches and isCritical true`() {
        // Given
        whenever(mockPluginContext.referenceFunctions(fakeCallableId)) doReturn
            listOf(mockSimpleFunctionSymbol, mockDatadogModifierFunctionSymbol)

        // When
        val result = pluginContextUtils.referenceSingleFunction(fakeCallableId, isCritical = true)

        // Then
        assertThat(result).isNull()
        val argumentCaptor = argumentCaptor<String>()
        verify(mockMessageCollector).report(
            eq(CompilerMessageSeverity.ERROR),
            argumentCaptor.capture(),
            isNull()
        )
        assertThat(argumentCaptor.firstValue).contains("has none or several references.")
    }

    @Test
    fun `M return null and log strong warning W referenceSingleFunction with multiple matches and isCritical false`() {
        // Given
        whenever(mockPluginContext.referenceFunctions(fakeCallableId)) doReturn
            listOf(mockSimpleFunctionSymbol, mockDatadogModifierFunctionSymbol)

        // When
        val result = pluginContextUtils.referenceSingleFunction(fakeCallableId, isCritical = false)

        // Then
        val argumentCaptor = argumentCaptor<String>()
        assertThat(result).isNull()
        verify(mockMessageCollector).report(
            eq(CompilerMessageSeverity.STRONG_WARNING),
            argumentCaptor.capture(),
            isNull()
        )
    }

    @Test
    fun `M return datadog modifier symbol W getDatadogModifierSymbol`() {
        // Given
        val datadogModifierCallableId = CallableId(
            packageName = FqName("com.datadog.android.compose"),
            callableName = Name.identifier("instrumentedDatadog")
        )
        whenever(mockPluginContext.referenceFunctions(datadogModifierCallableId)) doReturn
            listOf(mockDatadogModifierFunctionSymbol)

        // When
        val result = pluginContextUtils.getDatadogModifierSymbol()

        // Then
        assertThat(result).isEqualTo(mockDatadogModifierFunctionSymbol)
    }

    @Test
    fun `M return modifier then symbol W getModifierThen`() {
        // Given
        val modifierThenCallableId = CallableId(
            ClassId(composeUiPackageName, modifierClassRelativeName),
            Name.identifier("then")
        )
        whenever(mockPluginContext.referenceFunctions(modifierThenCallableId)) doReturn
            listOf(mockSimpleFunctionSymbol)

        // When
        val result = pluginContextUtils.getModifierThen()

        // Then
        assertThat(result).isEqualTo(mockSimpleFunctionSymbol)
    }

    @Test
    fun `M return datadog track effect symbol W getDatadogTrackEffectSymbol`() {
        // Given
        val datadogTrackEffectCallableId = CallableId(
            FqName("com.datadog.android.compose"),
            Name.identifier("InstrumentedNavigationViewTrackingEffect")
        )
        whenever(mockPluginContext.referenceFunctions(datadogTrackEffectCallableId)) doReturn
            listOf(mockSimpleFunctionSymbol)

        // When
        val result = pluginContextUtils.getDatadogTrackEffectSymbol()

        // Then
        assertThat(result).isEqualTo(mockSimpleFunctionSymbol)
    }

    @Test
    fun `M return nav host controller class symbol W getNavHostControllerClassSymbol`() {
        // Given
        val navHostControllerClassId = ClassId(
            FqName("androidx.navigation"),
            Name.identifier("NavHostController")
        )
        whenever(mockPluginContext.referenceClass(navHostControllerClassId)) doReturn
            mockNavHostControllerClassSymbol

        // When
        val result = pluginContextUtils.getNavHostControllerClassSymbol()

        // Then
        assertThat(result).isEqualTo(mockNavHostControllerClassSymbol)
    }

    @Test
    fun `M return null and log warning W getNavHostControllerClassSymbol and class not found`() {
        // Given
        val navHostControllerClassId = ClassId(
            FqName("androidx.navigation"),
            Name.identifier("NavHostController")
        )
        whenever(mockPluginContext.referenceClass(navHostControllerClassId)) doReturn null

        // When
        val result = pluginContextUtils.getNavHostControllerClassSymbol()

        // Then
        assertThat(result).isNull()
        verify(mockMessageCollector).report(
            CompilerMessageSeverity.WARNING,
            "androidx/navigation/NavHostController is not found."
        )
    }

    @Test
    fun `M return apply symbol W getApplySymbol`() {
        // Given
        val applyCallableId = CallableId(
            FqName("kotlin"),
            Name.identifier("apply")
        )
        whenever(mockPluginContext.referenceFunctions(applyCallableId)) doReturn
            listOf(mockSimpleFunctionSymbol)

        // When
        val result = pluginContextUtils.getApplySymbol()

        // Then
        assertThat(result).isEqualTo(mockSimpleFunctionSymbol)
    }

    @Test
    fun `M return false W isComposableFunction with androidx package`() {
        // Given
        whenever(mockIrFunction.parent) doReturn mockParent
        whenever(mockParent.kotlinFqName) doReturn FqName("androidx.compose.foundation")

        // When
        val result = pluginContextUtils.isComposableFunction(mockIrFunction)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return false W isComposableFunction without composable annotation`() {
        // Given
        whenever(mockIrFunction.parent) doReturn mockParent
        whenever(mockParent.kotlinFqName) doReturn FqName("com.example")
        setupNoMatchingAnnotation()

        // When
        val result = pluginContextUtils.isComposableFunction(mockIrFunction)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return true W isFoundationImage with Image function in foundation package`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Image")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.foundation")

        // When
        val result = pluginContextUtils.isFoundationImage(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `M return false W isFoundationImage with wrong function name`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Button")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.foundation")

        // When
        val result = pluginContextUtils.isFoundationImage(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return false W isFoundationImage with wrong package`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Image")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.material")

        // When
        val result = pluginContextUtils.isFoundationImage(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return true W isMaterialIcon with Icon function in material package`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Icon")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.material")

        // When
        val result = pluginContextUtils.isMaterialIcon(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `M return true W isMaterialIcon with Icon function in material3 package`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Icon")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.material3")

        // When
        val result = pluginContextUtils.isMaterialIcon(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `M return false W isMaterialIcon with wrong function name`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Button")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.material")

        // When
        val result = pluginContextUtils.isMaterialIcon(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return false W isMaterialIcon with wrong package`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Icon")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.foundation")

        // When
        val result = pluginContextUtils.isMaterialIcon(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return true W isCoilAsyncImage with AsyncImage function in coil package`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("AsyncImage")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("coil.compose")

        // When
        val result = pluginContextUtils.isCoilAsyncImage(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `M return false W isCoilAsyncImage with wrong function name`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("Image")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("coil.compose")

        // When
        val result = pluginContextUtils.isCoilAsyncImage(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return false W isCoilAsyncImage with wrong package`() {
        // Given
        whenever(mockIrFunction.name) doReturn Name.identifier("AsyncImage")
        whenever(mockIrPackageFragment.packageFqName) doReturn FqName("androidx.compose.foundation")

        // When
        val result = pluginContextUtils.isCoilAsyncImage(mockIrFunction, mockIrPackageFragment)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `M return false W isComposeInstrumentationTargetFunc with non-composable function`() {
        // Given
        whenever(mockIrFunction.parent) doReturn mockParent
        whenever(mockParent.kotlinFqName) doReturn FqName("com.example")
        setupNoMatchingAnnotation()

        // When
        val result =
            pluginContextUtils.isComposeInstrumentationTargetFunc(mockIrFunction, annotationModeEnabled = false)

        // Then
        assertThat(result).isFalse()
    }

    companion object {
        private val composeUiPackageName = FqName("androidx.compose.ui")
        private val modifierClassRelativeName = Name.identifier("Modifier")
    }
}
