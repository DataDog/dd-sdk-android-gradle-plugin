/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import fr.xgouchet.elmyr.annotation.BoolForgery
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
internal class ComposeNavHostExtension21Test : ComposeNavHostExtensionTest() {

    @Test
    override fun `M accept ComposeNavHostTransformer W generate`(@BoolForgery annotationModeEnabled: Boolean) {
        val composeNavHostExtension = ComposeNavHostExtension21(
            messageCollector = mockMessageCollector,
            annotationModeEnabled = annotationModeEnabled
        )

        // When
        composeNavHostExtension.generate(mockModuleFragment, mockPluginContext)

        // Then
        verify(mockModuleFragment).accept(any<ComposeNavHostTransformer21>(), eq(null))
    }
}
