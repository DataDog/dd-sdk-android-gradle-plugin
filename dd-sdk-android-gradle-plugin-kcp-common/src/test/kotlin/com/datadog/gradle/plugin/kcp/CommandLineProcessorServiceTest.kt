/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

internal class CommandLineProcessorServiceTest {

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `M discover DatadogKotlinCompilerPluginCommandLineProcessor W ServiceLoader loads`() {
        val processors = ServiceLoader.load(CommandLineProcessor::class.java)
            .filter { it.javaClass.name.startsWith("com.datadog") }
        assertThat(processors)
            .singleElement()
            .isInstanceOf(DatadogKotlinCompilerPluginCommandLineProcessor::class.java)
    }
}
