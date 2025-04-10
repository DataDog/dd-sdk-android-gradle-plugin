package com.datadog.gradle.plugin

/**
 * Defines the mode of instrumentation for the plugin.
 *
 * This enum controls how the plugin applies instrumentation to the code, based on the selected mode.
 */
enum class InstrumentationMode {

    /**
     * **AUTO** mode enables automatic instrumentation.
     *
     * In this mode, the plugin automatically instruments all required code
     * for the feature without requiring explicit annotations.
     */
    AUTO,

    /**
     * **ANNOTATION** mode enables selective instrumentation.
     *
     * In this mode, only functions explicitly annotated with the required annotation
     * will be instrumented by the plugin.
     */
    ANNOTATION,

    /**
     * **DISABLE** mode turns off instrumentation.
     *
     * When this mode is set, the plugin does not apply any instrumentation
     * for this feature, effectively disabling it.
     */
    DISABLE;

    companion object {
        internal fun from(value: String): InstrumentationMode? {
            return entries.firstOrNull { it.name == value }
        }
    }
}
