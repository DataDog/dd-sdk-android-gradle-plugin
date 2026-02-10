/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode

class ComposeNavHostCompilation22Test : ComposeNavHostCompilationTest() {
    override fun getDatadogPluginRegistrar(instrumentationMode: InstrumentationMode): DatadogPluginRegistrar {
        return DatadogPluginRegistrarImpl(instrumentationMode)
    }
}
