package com.datadog.gradle.plugin

import com.android.builder.model.Version
import org.gradle.api.Project
import java.io.File

internal object DdTaskUtils {
    private const val MAX_DATADOG_CI_FILE_LOOKUP_LEVELS = 4

    @Suppress("StringLiteralDuplication")
    fun resolveDatadogRepositoryFile(target: Project): File {
        val outputsDir = File(target.buildDir, "outputs")
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

    @Suppress("MagicNumber", "ReturnCount")
    fun isAgpAbove(major: Int, minor: Int, patch: Int): Boolean {
        val version = Version.ANDROID_GRADLE_PLUGIN_VERSION
        val groups = version.split(".")
        if (groups.size < 3) return false
        val currentMajor = groups[0].toIntOrNull()
        val currentMinor = groups[1].toIntOrNull()
        val currentPatch = groups[2].substringBefore("-").toIntOrNull()
        if (currentMajor == null || currentMinor == null || currentPatch == null) return false
        return currentMajor >= major && currentMinor >= minor && currentPatch >= patch
    }
}
