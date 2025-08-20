/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.android.builder.model.Version
import org.gradle.api.Project
import java.io.File

internal object TaskUtils {

    private const val MAX_DATADOG_CI_FILE_LOOKUP_LEVELS = 4

    @Suppress("StringLiteralDuplication")
    fun resolveDatadogRepositoryFile(target: Project): File {
        val outputsDir = target.layout.buildDirectory.dir("outputs").get().asFile
        val reportsDir = File(outputsDir, "reports")
        val datadogDir = File(reportsDir, "datadog")
        return File(datadogDir, "repository.json")
    }

    fun findDatadogCiFile(projectDir: File): File? {
        var currentDir: File? = projectDir
        var levelsUp = 0
        while (currentDir != null && levelsUp < MAX_DATADOG_CI_FILE_LOOKUP_LEVELS) {
            val datadogCiFile = File(currentDir, "datadog-ci.json")
            if (datadogCiFile.exists()) {
                return datadogCiFile
            }
            currentDir = currentDir.parentFile
            levelsUp++
        }
        return null
    }

    fun isAgpAbove(major: Int, minor: Int, patch: Int): Boolean {
        return isVersionAbove(Version.ANDROID_GRADLE_PLUGIN_VERSION, major, minor, patch)
    }

    // Gradle version may not have patch version
    fun isGradleAbove(project: Project, major: Int, minor: Int, patch: Int = 0): Boolean {
        return isVersionAbove(project.gradle.gradleVersion, major, minor, patch)
    }

    @Suppress("MagicNumber", "ReturnCount")
    private fun isVersionAbove(refVersion: String, major: Int, minor: Int, patch: Int): Boolean {
        val groups = refVersion.split(".")
        // should be at least major and minor versions
        if (groups.size < 2) return false
        val currentMajor = groups[0].toIntOrNull() ?: 0
        val currentMinor = groups[1].toIntOrNull() ?: 0
        val currentPatch = groups.getOrNull(2)?.substringBefore("-")?.toIntOrNull() ?: 0
        return currentMajor >= major && currentMinor >= minor && currentPatch >= patch
    }
}

@Deprecated(
    message = "Renamed to MappingFileUploadTask",
    replaceWith = ReplaceWith(
        expression = "MappingFileUploadTask",
        imports = arrayOf("com.datadog.gradle.plugin.MappingFileUploadTask")
    )
)
@Suppress("unused")
typealias DdMappingFileUploadTask = MappingFileUploadTask

@Deprecated(
    message = "Renamed to CheckSdkDepsTask",
    replaceWith = ReplaceWith(
        expression = "CheckSdkDepsTask",
        imports = arrayOf("com.datadog.gradle.plugin.CheckSdkDepsTask")
    )
)
@Suppress("unused")
typealias DdCheckSdkDepsTask = CheckSdkDepsTask
