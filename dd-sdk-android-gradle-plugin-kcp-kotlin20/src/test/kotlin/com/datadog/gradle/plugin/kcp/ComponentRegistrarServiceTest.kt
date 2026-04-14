/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

@file:Suppress("deprecation")

package com.datadog.gradle.plugin.kcp

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

internal class ComponentRegistrarServiceTest {

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `M discover DatadogPluginRegistrarImpl W ServiceLoader loads`() {
        val registrars = ServiceLoader.load(ComponentRegistrar::class.java).toList()
        assertThat(registrars).anyMatch { it is DatadogPluginRegistrarImpl }
    }
}
