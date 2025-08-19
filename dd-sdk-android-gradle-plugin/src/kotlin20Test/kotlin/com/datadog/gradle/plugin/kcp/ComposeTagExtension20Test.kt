package com.datadog.gradle.plugin.kcp

import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.kotlin.any
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify

@Extensions(
    ExtendWith(ForgeExtension::class)
)
@OptIn(FirIncompatiblePluginAPI::class)
internal class ComposeTagExtension20Test : ComposeTagExtensionTest() {

    @Test
    override fun `M accept ComposeTagTransformer W generate`(@BoolForgery annotationModeEnabled: Boolean) {
        // Given
        val composeTagExtension = ComposeTagExtension20(
            messageCollector = mockMessageCollector,
            annotationModeEnabled = annotationModeEnabled
        )

        // When
        composeTagExtension.generate(
            moduleFragment = mockModuleFragment,
            pluginContext = mockPluginContext
        )

        // Then
        verify(mockModuleFragment).accept(
            any<ComposeTagTransformer20>(),
            isNull()
        )
    }
}
