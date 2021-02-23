package com.datadog.gradle.plugin.internal

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
            project.execShell("git", "rev-parse", "--is-inside-work-tree").toBoolean()
        } catch (e: ExecException) {
            System.err.println("Project is not a git repository: ${e.message}")
            return emptyList()
        }

        val remoteUrl = project.execShell("git", "remote", "get-url", "origin").trim()
        val commitHash = project.execShell("git", "rev-parse", "HEAD").trim()
        val rootFolder = project.execShell("git", "rev-parse", "--show-toplevel").trim()
        val rootPathPrefix = rootFolder + File.separator

        val trackedFiles = listTrackedFilesPath(sourceSetRoots, rootPathPrefix)

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
        sourceSetRoots: List<File>,
        rootPathPrefix: String
    ): List<String> {
        val files = mutableListOf<String>()
        sourceSetRoots.forEach { root ->
            if (root.exists() && root.isDirectory) {
                listFilePathsInFolder(root, rootPathPrefix, files)
            }
        }
        return files
    }

    private fun listFilePathsInFolder(
        root: File,
        rootPathPrefix: String,
        files: MutableList<String>
    ) {
        root.walkTopDown()
            .forEach {
                if (it.isFile) {
                    val localPath = it.absolutePath.substringAfter(rootPathPrefix)
                    files.add(localPath)
                }
            }
    }

    // endregion

    companion object {
        private const val GIT_CMD = "git"
    }
}
