package com.datadog.gradle.plugin.internal.sanitizer

internal class ShortUriGitRemoteUrlSanitizer : FullUriGitRemoteUrlSanitizer() {
    override fun sanitize(url: String): String {
        val sanitizedUri = super.sanitize("$SSH_SCHEMA_PREFIX$url")
        return sanitizedUri.removePrefix(SSH_SCHEMA_PREFIX)
    }

    companion object {
        const val SSH_SCHEMA_PREFIX = "ssh://"
    }
}
