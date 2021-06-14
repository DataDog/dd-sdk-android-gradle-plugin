package com.datadog.gradle.plugin.internal.sanitizer

import java.lang.IllegalArgumentException

internal class UriParsingException(message: String, throwable: Throwable) :
    IllegalArgumentException(message, throwable)
