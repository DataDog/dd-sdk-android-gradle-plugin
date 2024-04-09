/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.utils

import java.io.File
import java.util.concurrent.TimeUnit

internal fun initializeGit(remoteUrl: String, rootDirectory: File) {
    val readme = File(rootDirectory, "README.md")
    readme.writeText("# Fake project")

    check(
        ProcessBuilder("git", "init")
            .directory(rootDirectory)
            .start()
            .waitForSuccess(5, TimeUnit.SECONDS)
    )
    check(
        ProcessBuilder("git", "add", ".")
            .directory(rootDirectory)
            .start()
            .waitForSuccess(5, TimeUnit.SECONDS)
    )
    check(
        ProcessBuilder("git", "config", "commit.gpgsign", "false")
            .directory(rootDirectory)
            .start()
            .waitForSuccess(5, TimeUnit.SECONDS)
    )
    check(
        ProcessBuilder("git", "config", "user.name", "\"Some User\"")
            .directory(rootDirectory)
            .start()
            .waitForSuccess(5, TimeUnit.SECONDS)
    )
    check(
        ProcessBuilder("git", "config", "user.email", "\"user@example.com\"")
            .directory(rootDirectory)
            .start()
            .waitForSuccess(5, TimeUnit.SECONDS)
    )
    check(
        ProcessBuilder("git", "commit", "-m", "Init")
            .directory(rootDirectory)
            .start()
            .waitForSuccess(5, TimeUnit.SECONDS)
    )
    check(
        ProcessBuilder("git", "remote", "add", "origin", remoteUrl)
            .directory(rootDirectory)
            .start()
            .waitForSuccess(5, TimeUnit.SECONDS)
    )
}

fun headHash(rootDirectory: File): String {
    val process = ProcessBuilder("git", "rev-parse", "HEAD")
        .directory(rootDirectory)
        .start()

    check(process.waitForSuccess(5, TimeUnit.SECONDS))

    return process.inputReader().readText().trim()
}

private fun Process.waitForSuccess(timeout: Long, unit: TimeUnit): Boolean {
    val haveNoTimeout = waitFor(timeout, unit)
    return haveNoTimeout && exitValue() == 0
}
