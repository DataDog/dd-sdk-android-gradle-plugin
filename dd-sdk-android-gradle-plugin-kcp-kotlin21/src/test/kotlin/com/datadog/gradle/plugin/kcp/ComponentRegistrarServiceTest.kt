/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

@OptIn(ExperimentalCompilerApi::class)
internal class ComponentRegistrarServiceTest {

    @Test
    fun `M discover DatadogPluginRegistrarImpl W ServiceLoader loads`() {
        // Verify the actual service used to run before Compose on Kotlin versions below 2.4.
        @Suppress("DEPRECATION")
        val providers = ServiceLoader.load(org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar::class.java)
            .filter { it.javaClass.name.startsWith("com.datadog.") }

        assertThat(providers)
            .singleElement()
            .isInstanceOf(DatadogPluginRegistrarImpl::class.java)
    }
}
