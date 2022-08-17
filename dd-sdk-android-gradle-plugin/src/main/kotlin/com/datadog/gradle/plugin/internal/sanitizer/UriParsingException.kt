/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.sanitizer

import java.lang.IllegalArgumentException

internal class UriParsingException(message: String, throwable: Throwable) :
    IllegalArgumentException(message, throwable)
