/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class DatadogKotlinCompilerPluginCommandLineProcessorTest {

    private val testedProcessor = DatadogKotlinCompilerPluginCommandLineProcessor()

    @Mock
    private lateinit var mockCompilerConfiguration: CompilerConfiguration

    @Test
    fun `M put value W option name is track actions`(
        @StringForgery fakeValue: String,
        @StringForgery fakeDescription: String
    ) {
        // Given
        val fakeCliOption: AbstractCliOption = CliOption(
            optionName = DatadogKotlinCompilerPluginCommandLineProcessor.INSTRUMENTATION_MODE,
            valueDescription = "<string>",
            description = fakeDescription
        )

        // When
        testedProcessor.processOption(fakeCliOption, fakeValue, mockCompilerConfiguration)

        // Then
        verify(mockCompilerConfiguration).put(DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE, fakeValue)
    }
}
