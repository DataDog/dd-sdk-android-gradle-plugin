package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.RepositoryDetector
import com.datadog.gradle.plugin.RepositoryInfo
import java.io.File
import javax.inject.Inject
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException

// TODO RUMM-1095 handle git submodules
// TODO RUMM-1096 handle git subtrees
// TODO RUMM-1093 let customer override `origin` with custom remote name
internal open class GitRepositoryDetector
@Inject constructor(
    @Suppress("UnstableApiUsage") private val execOperations: ExecOperations
) : RepositoryDetector {

    @Suppress("StringLiteralDuplication")
    override fun detectRepositories(
        sourceSetRoots: List<File>
    ): List<RepositoryInfo> {
        try {
            execOperations.execShell("git", "rev-parse", "--is-inside-work-tree")
        } catch (e: ExecException) {
            LOGGER.error("Project is not a git repository", e)
            return emptyList()
        }

        val remoteUrl = execOperations.execShell("git", "remote", "get-url", "origin").trim()
        val commitHash = execOperations.execShell("git", "rev-parse", "HEAD").trim()

        val trackedFiles = listTrackedFilesPath(sourceSetRoots)

        return listOf(
            RepositoryInfo(
                remoteUrl,
                commitHash,
                trackedFiles
            )
        )
    }

    // region Internal

    private fun listTrackedFilesPath(
        sourceSetRoots: List<File>
    ): List<String> {
        val files = mutableListOf<String>()
        sourceSetRoots.forEach { sourceSetRoot ->
            if (sourceSetRoot.exists() && sourceSetRoot.isDirectory) {
                listFilePathsInFolder(sourceSetRoot, files)
            }
        }
        return files
    }

    private fun listFilePathsInFolder(
        sourceSetRoot: File,
        files: MutableList<String>
    ) {

        // output will be relative to the project root
        val sourceSetFiles = execOperations.execShell(
            "git",
            "ls-files",
            sourceSetRoot.absolutePath
        )
            .trim()
            .lines()
            // we could use --deduplicate, but it was added to git just recently
            .toSet()

        files.addAll(sourceSetFiles)
    }

    // endregion
}
