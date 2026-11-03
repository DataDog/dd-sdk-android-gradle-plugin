/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

@OptIn(ExperimentalCompilerApi::class)
internal class CompilerPluginRegistrarServiceTest {

    @Test
    fun `M discover DatadogPluginRegistrarImpl W ServiceLoader loads`() {
        val providers = ServiceLoader.load(CompilerPluginRegistrar::class.java)
            .filter { it.javaClass.name.startsWith("com.datadog.") }

        assertThat(providers)
            .singleElement()
            .isInstanceOf(DatadogPluginRegistrarImpl::class.java)
    }
}
