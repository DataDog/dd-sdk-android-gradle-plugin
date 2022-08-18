/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.sanitizer

import java.util.Locale

/**
 * It will strip out the user information from the provided URL by applying the following rules:
 * 1. In case the URL is a valid schema URL : ((ssh|http[s]?):\\) it will strip out the user
 * information if any from it
 * 2. In case the URL is a valid schema URL but by mistake the user forgot to add the schema:
 * [username]:[password]@github.com/(.+).git it will strip out the user information if any and will
 * return the new url
 * 3. In case the URL is of a GIT SSH short format: [user@]github.com:(.+).git it will do
 * nothing assuming that there is no password normally provided in a GIT SSH short format URL.
 */
internal class GitRemoteUrlSanitizer(
    private val sanitizerResolver: (String) -> UrlSanitizer = {
        if (it.matches(VALID_SCHEMA_URL_FORMAT_REGEX)) {
            FullUriGitRemoteUrlSanitizer()
        } else {
            ShortUriGitRemoteUrlSanitizer()
        }
    }
) : UrlSanitizer {

    @SuppressWarnings("TooGenericExceptionCaught")
    override fun sanitize(url: String): String {
        try {
            return sanitizerResolver(url).sanitize(url)
        } catch (e: Exception) {
            throw UriParsingException(WRONG_URL_FORMAT_ERROR_MESSAGE.format(url, Locale.US), e)
        }
    }

    companion object {
        const val WRONG_URL_FORMAT_ERROR_MESSAGE =
            "The detected GIT remote url: [%s] is not a valid GIT url and it " +
                "will not be browsable from the Error Tracking panel. " +
                "You can provide a different URL through the plugin extension."
        const val VALID_SCHEMA_URL_FORMAT_PATTERN = "^(ssh|http|https)://(.+)$"
        val VALID_SCHEMA_URL_FORMAT_REGEX =
            Regex(VALID_SCHEMA_URL_FORMAT_PATTERN, RegexOption.IGNORE_CASE)
    }
}
