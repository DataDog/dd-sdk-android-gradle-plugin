package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.kcp.DatadogKotlinCompilerPluginCommandLineProcessor.Companion.INSTRUMENTATION_MODE
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
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
@ForgeConfiguration(Configurator::class)
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
            optionName = INSTRUMENTATION_MODE,
            valueDescription = "<string>",
            description = fakeDescription
        )

        // When
        testedProcessor.processOption(fakeCliOption, fakeValue, mockCompilerConfiguration)

        // Then
        verify(mockCompilerConfiguration).put(DatadogPluginRegistrar.CONFIG_INSTRUMENTATION_MODE, fakeValue)
    }
}
