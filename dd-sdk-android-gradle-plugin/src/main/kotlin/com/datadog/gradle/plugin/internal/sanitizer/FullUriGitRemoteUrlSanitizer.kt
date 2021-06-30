package com.datadog.gradle.plugin.internal.sanitizer

import java.net.URI
import java.net.URISyntaxException
import kotlin.jvm.Throws

internal open class FullUriGitRemoteUrlSanitizer : UrlSanitizer {
    @Throws(exceptionClasses = [URISyntaxException::class])
    override fun sanitize(url: String): String {
        val parsedUri = URI(url)
        return if (parsedUri.userInfo.isNullOrEmpty()) {
            url
        } else {
            val sanitizedUri = URI(
                parsedUri.scheme,
                null,
                parsedUri.host,
                parsedUri.port,
                parsedUri.path,
                parsedUri.query,
                parsedUri.fragment
            )
            sanitizedUri.toString()
        }
    }
}
