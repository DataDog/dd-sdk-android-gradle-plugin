package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode

internal data class InternalCompilerConfiguration(
    val trackViews: InstrumentationMode = InstrumentationMode.DISABLE,
    val trackActions: InstrumentationMode = InstrumentationMode.DISABLE,
    val recordImages: InstrumentationMode = InstrumentationMode.DISABLE
)
