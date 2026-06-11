/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class ComposeNavHostCompilation24Test : ComposeNavHostCompilationTest() {
    override fun registerDatadogPluginRegistrar(
        compilation: KotlinCompilation,
        instrumentationMode: InstrumentationMode
    ) {
        compilation.compilerPluginRegistrars = listOf(DatadogPluginRegistrarImpl(instrumentationMode))
    }
}
