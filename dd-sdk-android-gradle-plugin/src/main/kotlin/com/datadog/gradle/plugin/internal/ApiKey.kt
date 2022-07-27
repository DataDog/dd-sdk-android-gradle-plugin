package com.datadog.gradle.plugin.internal

internal data class ApiKey(val value: String, val source: ApiKeySource) {
    companion object {
        val NONE = ApiKey("", ApiKeySource.NONE)
    }
}

/**
 * Source of API key.
 */
enum class ApiKeySource {
    GRADLE_PROPERTY,
    ENVIRONMENT,
    DATADOG_CI_CONFIG_FILE,
    NONE
}
