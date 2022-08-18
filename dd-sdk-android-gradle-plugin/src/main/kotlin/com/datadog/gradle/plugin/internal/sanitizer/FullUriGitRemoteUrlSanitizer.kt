/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

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
