package com.datadog.gradle.plugin.kcp

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector

internal fun MessageCollector.abortError() {
    report(CompilerMessageSeverity.ERROR, ERROR_MISSING_DEP)
}

private const val ERROR_MISSING_DEP =
    "The Datadog Compose Instrumentation failed to initialize references and will abort.\n" +
        "To proceed, you can either set `composeInstrumentation` to `DISABLE` or remove the " +
        "`composeInstrumentation` setting entirely."
