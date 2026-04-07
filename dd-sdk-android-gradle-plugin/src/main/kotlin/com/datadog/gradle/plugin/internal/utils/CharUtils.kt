/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.utils

import java.util.Locale

internal fun capitalizeChar(char: Char): String {
    return if (char.isLowerCase()) {
        char.titlecase(Locale.US)
    } else {
        char.toString()
    }
}
