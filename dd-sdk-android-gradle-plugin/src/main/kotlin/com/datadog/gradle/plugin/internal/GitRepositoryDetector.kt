package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.RepositoryDetector
import com.datadog.gradle.plugin.RepositoryInfo
import java.io.File
import org.gradle.api.Project
import org.gradle.process.internal.ExecException

// TODO RUMM-1095 handle git submodules
// TODO RUMM-1096 handle git subtrees
// TODO RUMM-1093 let customer override `origin` with custom remote name
internal class GitRepositoryDetector : RepositoryDetector {

    @Suppress("StringLiteralDuplication")
    override fun detectRepositories(
        project: Project,
        sourceSetRoots: List<File>
    ): List<RepositoryInfo> {
        try {
            project.execShell("git", "rev-parse", "--is-inside-work-tree")
        } catch (e: ExecException) {
            LOGGER.error("Project is not a git repository", e)
            return emptyList()
        }

        val remoteUrl = project.execShell("git", "remote", "get-url", "origin").trim()
        val commitHash = project.execShell("git", "rev-parse", "HEAD").trim()

        val trackedFiles = listTrackedFilesPath(project, sourceSetRoots)

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
        project: Project,
        sourceSetRoots: List<File>
    ): List<String> {
        val files = mutableListOf<String>()
        sourceSetRoots.forEach { sourceSetRoot ->
            if (sourceSetRoot.exists() && sourceSetRoot.isDirectory) {
                listFilePathsInFolder(project, sourceSetRoot, files)
            }
        }
        return files
    }

    private fun listFilePathsInFolder(
        project: Project,
        sourceSetRoot: File,
        files: MutableList<String>
    ) {

        // output will be relative to the project root
        val sourceSetFiles = project.execShell(
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
