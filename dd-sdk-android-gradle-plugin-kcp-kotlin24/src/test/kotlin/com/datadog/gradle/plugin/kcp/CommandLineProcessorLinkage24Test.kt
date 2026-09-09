/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

/**
 * Guards the option-parsing path against the legacy `ComponentRegistrar` supertype.
 *
 * Kotlin 2.4.20 removed `org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar`, which
 * [DatadogPluginRegistrar] extends. Reading a compiler configuration key through that class from the
 * 2.4 code path makes the JVM resolve the missing supertype and fail with a [NoClassDefFoundError]
 * before any user code is instrumented. This test walks the same route the compiler takes -- service
 * loading the processor, then handing it an option -- so the linkage breaks here rather than in a
 * consumer build.
 */
@OptIn(ExperimentalCompilerApi::class, CompilerConfiguration.Internals::class)
internal class CommandLineProcessorLinkage24Test {

    @Test
    fun `M resolve instrumentation mode key W legacy ComponentRegistrar is absent from compiler`() {
        // Given
        assertThat(isLegacyComponentRegistrarPresent())
            .withFailMessage(
                "This test only has teeth on a compiler that dropped ComponentRegistrar." +
                    " Found it on the test classpath, so the Kotlin version under test is too old."
            )
            .isFalse()
        val testedProcessor = ServiceLoader
            .load(CommandLineProcessor::class.java, javaClass.classLoader)
            .single { it.pluginId == PLUGIN_ID }
        val fakeOption = testedProcessor.pluginOptions.single { it.optionName == INSTRUMENTATION_MODE }
        val fakeValue = InstrumentationMode.AUTO.name
        val configuration = CompilerConfiguration()

        // When
        testedProcessor.processOption(fakeOption, fakeValue, configuration)

        // Then
        assertThat(configuration[DatadogCompilerConfigurationKeys.CONFIG_INSTRUMENTATION_MODE])
            .isEqualTo(fakeValue)
    }

    private fun isLegacyComponentRegistrarPresent(): Boolean {
        return try {
            Class.forName(LEGACY_COMPONENT_REGISTRAR, false, javaClass.classLoader)
            true
        } catch (@Suppress("SwallowedException") e: ClassNotFoundException) {
            false
        }
    }

    companion object {
        private const val PLUGIN_ID = "com.datadoghq.kotlin.compiler"
        private const val INSTRUMENTATION_MODE = "INSTRUMENTATION_MODE"
        private const val LEGACY_COMPONENT_REGISTRAR =
            "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar"
    }
}
