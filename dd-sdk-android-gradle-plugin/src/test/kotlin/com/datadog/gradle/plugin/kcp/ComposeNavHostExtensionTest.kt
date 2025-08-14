package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@OptIn(FirIncompatiblePluginAPI::class, UnsafeDuringIrConstructionAPI::class)
@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class ComposeNavHostExtensionTest {

    private lateinit var testedExtension: ComposeNavHostExtension21

    @Mock
    private lateinit var mockMessageCollector: MessageCollector

    @Mock
    private lateinit var mockModuleFragment: IrModuleFragment

    @Mock
    private lateinit var mockPluginContext: IrPluginContext

    @Mock
    private lateinit var mockIrSimpleFunctionSymbol: IrSimpleFunctionSymbol

    @Mock
    private lateinit var mockClassSymbol: IrClassSymbol

    @BeforeEach
    fun `set up`() {
        // Deep mock in PluginContext to make `initReference` pass for each transformer
        val mockOwner = mock<IrClass>()
        val mockCompanionObject = mock<IrClass>()
        val mockCompanionSymbol = mock<IrClassSymbol>()
        whenever(mockPluginContext.referenceFunctions(any<CallableId>())) doReturn listOf(mockIrSimpleFunctionSymbol)
        whenever(mockPluginContext.referenceClass(any<ClassId>())) doReturn mockClassSymbol
        whenever(mockClassSymbol.owner) doReturn mockOwner
        whenever(mockOwner.declarations) doReturn mutableListOf(mockCompanionObject)
        whenever(mockCompanionObject.isCompanion) doReturn true
        whenever(mockCompanionObject.symbol) doReturn mockCompanionSymbol
    }

    @Test
    fun `M accept ComposeNavHostTransformer20 W generate`(@BoolForgery annotationModeEnabled: Boolean) {
        // Given
        testedExtension = ComposeNavHostExtension21(
            messageCollector = mockMessageCollector,
            annotationModeEnabled = annotationModeEnabled
        )

        // When
        testedExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment).accept(any<ComposeNavHostTransformer21>(), eq(null))
    }
}
