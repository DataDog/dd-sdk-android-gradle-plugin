package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.RepositoryDetector
import com.datadog.gradle.plugin.RepositoryInfo
import com.datadog.gradle.plugin.internal.sanitizer.GitRemoteUrlSanitizer
import com.datadog.gradle.plugin.internal.sanitizer.UrlSanitizer
import java.io.File
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException

// TODO RUMM-1095 handle git submodules
// TODO RUMM-1096 handle git subtrees
// TODO RUMM-1093 let customer override `origin` with custom remote name
internal class GitRepositoryDetector(
    @Suppress("UnstableApiUsage") private val execOperations: ExecOperations,
    private val urlSanitizer: UrlSanitizer = GitRemoteUrlSanitizer()
) : RepositoryDetector {

    @Suppress("StringLiteralDuplication")
    override fun detectRepositories(
        sourceSetRoots: List<File>,
        extensionProvidedRemoteUrl: String
    ): List<RepositoryInfo> {
        try {
            execOperations.execShell("git", "rev-parse", "--is-inside-work-tree")
        } catch (e: ExecException) {
            LOGGER.error("Project is not a git repository", e)
            return emptyList()
        }

        val remoteUrl = sanitizeUrl(resolveRemoteUrl(extensionProvidedRemoteUrl))
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
    private fun sanitizeUrl(remoteUrl: String): String {
        return urlSanitizer.sanitize(remoteUrl)
    }

    private fun resolveRemoteUrl(extensionProvidedRemoteUrl: String) =
        if (extensionProvidedRemoteUrl.isNotEmpty()) {
            extensionProvidedRemoteUrl
        } else {
            execOperations.execShell("git", "remote", "get-url", "origin").trim()
        }

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
