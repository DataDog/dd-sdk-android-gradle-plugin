/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Configuration read from the `datadog-ci.json` file.
 */
internal data class DatadogCiConfig(val site: String?, val apiKey: String?) {

    internal companion object {
        internal const val API_KEY_PROPERTY = "apiKey"
        internal const val SITE_PROPERTY = "datadogSite"

        fun parse(file: File): DatadogCiConfig? {
            return try {
                val config = JSONObject(file.readText())
                DatadogCiConfig(
                    site = config.optString(SITE_PROPERTY, null),
                    apiKey = config.optString(API_KEY_PROPERTY, null)
                )
            } catch (jsonException: JSONException) {
                LOGGER.warn("Failed to parse datadog-ci.json", jsonException)
                null
            } catch (ioException: IOException) {
                LOGGER.warn("Failed to read datadog-ci.json file", ioException)
                null
            }
        }
    }
}
