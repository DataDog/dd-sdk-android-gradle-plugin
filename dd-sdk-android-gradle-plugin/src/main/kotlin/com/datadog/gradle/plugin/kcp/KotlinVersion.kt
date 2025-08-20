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

            return when {
                major > 2 || (major == 2 && minor >= 2) -> KOTLIN22
                major == 2 && minor == 1 && patch >= 20 -> KOTLIN21
                else -> KOTLIN20
            }
        }
    }
}
