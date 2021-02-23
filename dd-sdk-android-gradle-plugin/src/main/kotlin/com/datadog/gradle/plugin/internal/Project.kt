package com.datadog.gradle.plugin.internal

import java.io.ByteArrayOutputStream
import org.gradle.api.Project
import org.gradle.process.internal.ExecException

internal fun Project.execShell(vararg command: String): String {
    val outputStream = ByteArrayOutputStream()
    val errorStream = ByteArrayOutputStream()
    try {
        this.exec {
            @Suppress("SpreadOperator")
            it.commandLine(*command)
            it.standardOutput = outputStream
            it.errorOutput = errorStream
            println(it.workingDir)
        }
    } catch (e: ExecException) {
        System.err.println(errorStream.toString("UTF-8"))
        throw e
    }

    return outputStream.toString("UTF-8")
}
