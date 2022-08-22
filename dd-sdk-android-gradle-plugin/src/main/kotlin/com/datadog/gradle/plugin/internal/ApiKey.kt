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
    GRADLE_PROPERTY,
    ENVIRONMENT,
    DATADOG_CI_CONFIG_FILE,
    NONE
}
