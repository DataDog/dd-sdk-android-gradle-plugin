package com.datadog.gradle.plugin

import java.io.File
import org.gradle.api.Project

/**
 * Describes a class which can detect information about the version control
 * repository associated with a Gradle [Project].
 */
interface RepositoryDetector {

    /**
     * Perform a local analysis of the repository.
     * @param project the Gradle [Project] to analyse
     * @param sourceSetRoots the list of relevant sourceSet root folders
     * @return a list of [RepositoryInfo] describing the underlying Gradle [Project]
     */
    fun detectRepositories(
        project: Project,
        sourceSetRoots: List<File>
    ): List<RepositoryInfo>
}
