/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

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
    /**
     * API key is coming from Gradle property.
     */
    GRADLE_PROPERTY,

    /**
     * API key is coming from environment property.
     */
    ENVIRONMENT,

    /**
     * API key is coming from datadog-ci.json file.
     */
    DATADOG_CI_CONFIG_FILE,

    /**
     * There is no API key.
     */
    NONE
}
