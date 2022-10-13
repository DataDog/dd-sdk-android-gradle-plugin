/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import org.gradle.api.Project
import java.io.File

/**
 * Describes a class which can detect information about the version control
 * repository associated with a Gradle [Project].
 */
interface RepositoryDetector {

    /**
     * Perform a local analysis of the repository.
     * @param sourceSetRoots the list of relevant sourceSet root folders
     * @param extensionProvidedRemoteUrl the remote repository url provided in the plugin extension. If this
     * value is empty (not provided) we will automatically extract the default URL from GIT
     * configuration.
     * @return a list of [RepositoryInfo] describing the underlying Gradle [Project]
     */
    fun detectRepositories(
        sourceSetRoots: List<File>,
        extensionProvidedRemoteUrl: String = ""
    ): List<RepositoryInfo>
}
