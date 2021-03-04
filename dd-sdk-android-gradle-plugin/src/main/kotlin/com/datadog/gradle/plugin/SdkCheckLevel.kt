package com.datadog.gradle.plugin

/**
 * Defines the action which plugin should take if Datadog SDK is missing
 * in the project dependencies.
 */
enum class SdkCheckLevel {
    /**
     * Do nothing if SDK is missing in the dependencies.
     */
    NONE,

    /**
     * Log a warning if SDK is missing in the dependencies.
     */
    WARN,

    /**
     * Fail the build if SDK is missing in the dependencies.
     */
    FAIL
}
