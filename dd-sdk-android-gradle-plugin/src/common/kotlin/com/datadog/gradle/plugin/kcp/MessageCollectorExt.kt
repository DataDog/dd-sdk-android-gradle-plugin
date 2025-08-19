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

private const val WARNING_MISSING_DEP =
    "The Datadog Plugin has detected missing dependencies required for Compose Instrumentation in this module. " +
        "Missing these dependencies may result in incomplete Compose Instrumentation functionality. " +
        "You can ignore this warning if you're certain that the missing dependencies " +
        "are not needed for your use case."
