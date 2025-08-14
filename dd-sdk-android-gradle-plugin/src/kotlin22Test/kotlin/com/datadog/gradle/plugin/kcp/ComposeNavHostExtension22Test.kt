package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@Extensions(
    ExtendWith(ForgeExtension::class)
)
@OptIn(FirIncompatiblePluginAPI::class, UnsafeDuringIrConstructionAPI::class)
@ForgeConfiguration(Configurator::class)
internal class ComposeNavHostExtension22Test : ComposeNavHostExtensionTest() {

    @Test
    override fun `M accept ComposeNavHostTransformer W generate`(@BoolForgery annotationModeEnabled: Boolean) {
        val composeNavHostExtension = ComposeNavHostExtension22(
            messageCollector = mockMessageCollector,
            annotationModeEnabled = annotationModeEnabled
        )

        // When
        composeNavHostExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment).accept(any<ComposeNavHostTransformer22>(), eq(null))
    }
}
