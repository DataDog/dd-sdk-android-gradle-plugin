package com.datadog.gradle.plugin

import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents the information on the version control repository associated
 * with the current Gradle Project.
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
