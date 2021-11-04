package com.datadog.gradle.plugin.utils

import java.util.Locale

fun capitalizeChar(char: Char): CharSequence {
    return if (char.isLowerCase()) {
        char.titlecase(Locale.US)
    } else {
        char.toString()
    }
}
