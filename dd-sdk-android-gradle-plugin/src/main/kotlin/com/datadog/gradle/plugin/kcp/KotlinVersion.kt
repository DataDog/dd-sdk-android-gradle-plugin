/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

internal enum class KotlinVersion {
    KOTLIN22,
    KOTLIN21,
    KOTLIN20;

    companion object {

        @Suppress("ReturnCount", "MagicNumber")
        fun from(versionString: String?): KotlinVersion {
            if (versionString == null) {
                return KOTLIN20 // Default or handle error as preferred
            }

            val versionParts =
                versionString.split('.').mapNotNull { it.substringBefore('-').toIntOrNull() }

            if (versionParts.size < 3) {
                return KOTLIN20 // Or handle malformed version string
            }

            val major = versionParts[0]
            val minor = versionParts[1]
            val patch = versionParts[2]

            // Kotlin 2.2+ and 2.3+ use KOTLIN22 (backward-compatible API in Ir22Ext.kt)
            return when {
                major > 2 || (major == 2 && minor >= 2) -> KOTLIN22
                major == 2 && minor == 1 && patch >= 20 -> KOTLIN21
                else -> KOTLIN20
            }
        }
    }
}
