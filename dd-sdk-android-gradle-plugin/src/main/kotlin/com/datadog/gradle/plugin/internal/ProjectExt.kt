package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import java.io.ByteArrayOutputStream
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException

@Suppress("UnstableApiUsage")
internal fun ExecOperations.execShell(vararg command: String): String {
    val outputStream = ByteArrayOutputStream()
    val errorStream = ByteArrayOutputStream()
    try {
        this.exec {
            @Suppress("SpreadOperator")
            it.commandLine(*command)
            it.standardOutput = outputStream
            it.errorOutput = errorStream
        }
    } catch (e: ExecException) {
        LOGGER.error(errorStream.toString("UTF-8"))
        throw e
    }

    return outputStream.toString("UTF-8")
}
