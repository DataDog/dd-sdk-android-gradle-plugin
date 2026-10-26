/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.TaskUtils
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Reads the API key from the `datadog-ci.json` file found in the project directory or its parents.
 *
 * It is a [ValueSource] rather than a plain provider, because the configuration cache stores the
 * value of plain providers, while a value source which is not queried at configuration time is
 * evaluated at execution time only, so the API key doesn't end up in the configuration cache.
 */
internal abstract class DatadogCiApiKeyValueSource :
    ValueSource<String, DatadogCiApiKeyValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val projectDirectory: DirectoryProperty
    }

    override fun obtain(): String? {
        val datadogCiFile = TaskUtils.findDatadogCiFile(parameters.projectDirectory.get().asFile)
            ?: return null
        return DatadogCiConfig.parse(datadogCiFile)?.apiKey?.ifEmpty { null }
    }
}
