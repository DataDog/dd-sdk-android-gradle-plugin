/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

internal enum class KotlinVersion {
    KOTLIN22,
    KOTLIN21,
    KOTLIN20,
    KOTLIN19,
    UNSUPPORTED;

    companion object {

        @Suppress("ReturnCount", "MagicNumber")
        fun from(versionString: String?): KotlinVersion {
            if (versionString == null) {
                return UNSUPPORTED // Default or handle error as preferred
            }

            val versionParts =
                versionString.split('.').mapNotNull { it.substringBefore('-').toIntOrNull() }

            if (versionParts.size < 3) {
                return UNSUPPORTED // Or handle malformed version string
            }

            val major = versionParts[0]
            val minor = versionParts[1]
            val patch = versionParts[2]

            return when {
                major == 1 && minor == 9 && patch >= 23 -> KOTLIN19
                major == 2 && minor == 1 -> KOTLIN21
                major == 2 && minor == 0 -> KOTLIN20
                major > 2 || (major == 2 && minor >= 2) -> KOTLIN22
                else -> UNSUPPORTED
            }
        }
    }
}
