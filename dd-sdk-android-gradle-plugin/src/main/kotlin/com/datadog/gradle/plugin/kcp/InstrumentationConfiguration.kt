package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode

/**
 * This class defines settings that control how the plugin applies instrumentation
 * for various features in the Gradle plugin configuration.
 */
class InstrumentationConfiguration {

    /**
     * Determines how RUM view tracking is instrumented.
     *
     * - **AUTO**: Automatically instruments views for tracking.
     * - **ANNOTATION**: Instruments only the composable functions explicitly annotated.
     * - **DISABLE**: Disables view tracking instrumentation.
     *
     * Default: **DISABLE**
     */
    var trackViews: InstrumentationMode = InstrumentationMode.DISABLE

    /**
     * Determines how RUM actions tracking is instrumented.
     *
     * - **AUTO**: Automatically instruments user actions for tracking.
     * - **ANNOTATION**: Instruments only the composable functions  explicitly annotated.
     * - **DISABLE**: Disables action tracking instrumentation.
     *
     * Default: **DISABLE**
     */
    var trackActions: InstrumentationMode = InstrumentationMode.DISABLE

    /**
     * Determines how Session Replay image recording is instrumented.
     *
     * - **AUTO**: Automatically instruments image-related code.
     * - **ANNOTATION**: Instruments only the composable functions explicitly annotated.
     * - **DISABLE**: Disables image recording instrumentation.
     *
     * Default: **DISABLE**
     */
    var recordImages: InstrumentationMode = InstrumentationMode.DISABLE
}
