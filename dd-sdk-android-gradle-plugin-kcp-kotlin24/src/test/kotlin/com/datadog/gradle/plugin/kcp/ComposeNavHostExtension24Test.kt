/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.ClassId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(FirIncompatiblePluginAPI::class, UnsafeDuringIrConstructionAPI::class)
internal class ComposeNavHostExtension24Test : ComposeNavHostExtensionTest() {

    @Mock
    private lateinit var mockIrConstructorSymbol: IrConstructorSymbol

    @BeforeEach
    fun setUpNavHostSymbols() {
        whenever(mockPluginContext.referenceConstructors(any<ClassId>())) doReturn listOf(mockIrConstructorSymbol)
    }

    override fun verifyTransformerAccepted(annotationModeEnabled: Boolean) {
        val composeNavHostExtension = ComposeNavHostExtension(
            messageCollector = mockMessageCollector,
            annotationModeEnabled = annotationModeEnabled
        )

        composeNavHostExtension.generate(mockModuleFragment, mockPluginContext)

        verify(mockModuleFragment).accept(any<ComposeNavHostTransformer>(), eq(null))
    }

    @Test
    fun `M accept ComposeNavHostTransformer W generate with annotation mode disabled`() {
        verifyTransformerAccepted(false)
    }

    @Test
    fun `M accept ComposeNavHostTransformer W generate with annotation mode enabled`() {
        verifyTransformerAccepted(true)
    }
}
