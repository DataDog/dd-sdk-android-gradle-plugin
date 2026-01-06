/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@OptIn(FirIncompatiblePluginAPI::class, UnsafeDuringIrConstructionAPI::class)
internal class ComposeTagExtension21Test : ComposeTagExtensionTest() {

    override fun verifyTransformerAccepted(annotationModeEnabled: Boolean) {
        val composeTagExtension = ComposeTagExtension21(
            messageCollector = mockMessageCollector,
            annotationModeEnabled = annotationModeEnabled
        )

        composeTagExtension.generate(mockModuleFragment, mockPluginContext)

        verify(mockModuleFragment).accept(any<ComposeTagTransformer21>(), eq(null))
    }

    @Test
    fun `M accept ComposeTagTransformer W generate with annotation mode disabled`() {
        verifyTransformerAccepted(false)
    }

    @Test
    fun `M accept ComposeTagTransformer W generate with annotation mode enabled`() {
        verifyTransformerAccepted(true)
    }
}
