package com.datadog.gradle.plugin.internal.sanitizer

internal interface UrlSanitizer {
    fun sanitize(url: String): String
}
