/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents the information on the version control repository associated
 * with the current Gradle Project.
 * @param url Repository URL.
 * @param hash Commit hash.
 * @param sourceFiles Source files at the given commit.
 * @see [RepositoryDetector]
 */
data class RepositoryInfo(
    val url: String,
    val hash: String,
    val sourceFiles: List<String>
) {
    /**
     * @return a JSON representation of this repository info.
     */
    fun toJson(): JSONObject {
        val repository = JSONObject()
        repository.put(KEY_REMOTE_URL, url)
        repository.put(KEY_COMMIT_HASH, hash)
        repository.put(KEY_TRACKED_FILES, JSONArray(sourceFiles))
        return repository
    }

    companion object {
        internal const val KEY_REMOTE_URL = "repository_url"
        internal const val KEY_COMMIT_HASH = "hash"
        internal const val KEY_TRACKED_FILES = "files"
    }
}
