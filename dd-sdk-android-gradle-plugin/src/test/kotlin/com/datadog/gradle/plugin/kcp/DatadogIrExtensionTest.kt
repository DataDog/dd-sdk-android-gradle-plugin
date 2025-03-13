package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@OptIn(FirIncompatiblePluginAPI::class)
@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class DatadogIrExtensionTest {

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
    fun `M register tag transformer W recordImages option is AUTO`(
        @Forgery fakeConfiguration: InternalCompilerConfiguration
    ) {
        // Given
        val datadogIrExtension = DatadogIrExtension(
            mockMessageCollector,
            fakeConfiguration.copy(
                recordImages = InstrumentationMode.AUTO
            )
        )

        // When
        datadogIrExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment).accept(any<ComposeTagTransformer>(), eq(null))
    }

    @Test
    fun `M register tag transformer W recordImages option is ANNOTATION`(
        @Forgery fakeConfiguration: InternalCompilerConfiguration
    ) {
        // Given
        val datadogIrExtension = DatadogIrExtension(
            mockMessageCollector,
            fakeConfiguration.copy(
                recordImages = InstrumentationMode.ANNOTATION
            )
        )

        // When
        datadogIrExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment).accept(any<ComposeTagTransformer>(), eq(null))
    }

    @Test
    fun `M not register tag transformer W recordImages option is DISABLE`(
        @Forgery fakeConfiguration: InternalCompilerConfiguration
    ) {
        // Given
        val datadogIrExtension = DatadogIrExtension(
            mockMessageCollector,
            fakeConfiguration.copy(
                recordImages = InstrumentationMode.DISABLE
            )
        )

        // When
        datadogIrExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment, never()).accept(any<ComposeTagTransformer>(), eq(null))
    }

    @Test
    fun `M register nav host transformer W track views option is AUTO`(
        @Forgery fakeConfiguration: InternalCompilerConfiguration
    ) {
        // Given
        val datadogIrExtension = DatadogIrExtension(
            mockMessageCollector,
            fakeConfiguration.copy(
                trackViews = InstrumentationMode.AUTO
            )
        )

        // When
        datadogIrExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment).accept(any<ComposeNavHostTransformer>(), eq(null))
    }

    @Test
    fun `M register nav host transformer W track views option is ANNOTATION`(
        @Forgery fakeConfiguration: InternalCompilerConfiguration
    ) {
        // Given
        val datadogIrExtension = DatadogIrExtension(
            mockMessageCollector,
            fakeConfiguration.copy(
                trackViews = InstrumentationMode.ANNOTATION
            )
        )

        // When
        datadogIrExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment).accept(any<ComposeNavHostTransformer>(), eq(null))
    }

    @Test
    fun `M not register nav host transformer W track views option is DISABLE`(
        @Forgery fakeConfiguration: InternalCompilerConfiguration
    ) {
        // Given
        val datadogIrExtension = DatadogIrExtension(
            mockMessageCollector,
            fakeConfiguration.copy(
                trackViews = InstrumentationMode.DISABLE
            )
        )

        // When
        datadogIrExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment, never()).accept(any<ComposeNavHostTransformer>(), eq(null))
    }
}
