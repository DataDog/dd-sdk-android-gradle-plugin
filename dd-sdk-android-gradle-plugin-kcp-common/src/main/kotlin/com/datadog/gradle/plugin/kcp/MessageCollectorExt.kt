/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

/**
 * Extension function to report a warning about missing dependencies for Compose Instrumentation.
 *
 * This function is used to notify the user that certain dependencies required for Compose Instrumentation
 * are missing in the current module, which may lead to incomplete functionality.
 *
 * @receiver MessageCollector - The message collector used to report the warning.
 */
fun MessageCollector.warnDependenciesError() {
    report(CompilerMessageSeverity.STRONG_WARNING, WARNING_MISSING_DEP)
}

/**
 * Extension function to report a message with info level.
 *
 * @receiver MessageCollector - The message collector used to report.
 * @param message message to report
 */
fun MessageCollector.info(message: String) {
    report(CompilerMessageSeverity.INFO, message)
}

/**
 * Extension function to report a message with strong warning level.
 *
 * @receiver MessageCollector - The message collector used to report.
 * @param message message to report
 */
fun MessageCollector.strongWarning(message: String) {
    report(CompilerMessageSeverity.STRONG_WARNING, message)
}

private const val WARNING_MISSING_DEP =
    "The Datadog Plugin has detected missing dependencies required for Compose Instrumentation in this module. " +
        "Missing these dependencies may result in incomplete Compose Instrumentation functionality. " +
        "You can ignore this warning if you're certain that the missing dependencies " +
        "are not needed for your use case."

/**
 * Error message of missing Datadog Jetpack Compose dependency.
 */
const val ERROR_MISSING_DATADOG_COMPOSE_INTEGRATION =
    "Missing com.datadoghq:dd-sdk-android-compose dependency."

/**
 * Error message of missing Android Compose UI dependency.
 */
const val ERROR_MISSING_COMPOSE_UI =
    "Missing androidx.compose.ui:ui dependency."

/**
 * Error message of missing Android Compose Navigation dependency.
 */
const val ERROR_MISSING_COMPOSE_NAV =
    "Missing androidx.navigation:navigation-compose dependency."

/**
 * Error message of missing Kotlin Stdlib dependency.
 */
const val ERROR_MISSING_KOTLIN_STDLIB =
    "Missing org.jetbrains.kotlin:kotlin-stdlib dependency."
