/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

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
internal class ComposeTagExtension23Test : ComposeTagExtensionTest() {

    @Test
    override fun `M accept ComposeTagTransformer W generate`(@BoolForgery annotationModeEnabled: Boolean) {
        // Given
        // Uses kotlin22 classes - API compatible with Kotlin 2.3.x
        val composeTagExtension = ComposeTagExtension22(
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
            any<ComposeTagTransformer22>(),
            isNull()
        )
    }
}
