/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.DdAppIdentifier
import com.datadog.gradle.plugin.internal.OkHttpUploader
import com.datadog.gradle.plugin.internal.Uploader
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * A Gradle task to upload a Proguard/R8 mapping file to Datadog servers.
 */
open class DdMappingFileUploadTask
@Inject constructor(
    @get:Internal internal val repositoryDetector: RepositoryDetector
) : DefaultTask() {

    @get:Internal
    internal var uploader: Uploader = OkHttpUploader()

    // TODO RUMM-2382 use Provider<String>
    /**
     * The API Key used to upload the data.
     */
    @get:Input
    var apiKey: String = ""

    /**
     * Source of the API key set: environment, gradle property, etc.
     */
    @get:Input
    var apiKeySource: ApiKeySource = ApiKeySource.NONE

    /**
     * The variant name of the application.
     */
    @get:Input
    var variantName: String = ""

    /**
     * The version name of the application.
     */
    @get:Input
    var versionName: String = ""

    /**
     * The service name of the application (by default, it is your app's package name).
     */
    @get:Input
    var serviceName: String = ""

    /**
     * The Datadog site to upload to (one of "US", "EU", "GOV").
     */
    @get:Input
    var site: String = ""

    /**
     * The url of the remote repository where the source code was deployed.
     */
    @get:Input
    var remoteRepositoryUrl: String = ""

    /**
     * The path to the mapping file to upload.
     */
    @get:Input
    var mappingFilePath: String = ""

    /**
     * Replacements for the source prefixes in the mapping file.
     */
    @get:Input
    var mappingFilePackagesAliases: Map<String, String> = emptyMap()

    /**
     * Trim indents in the mapping file.
     */
    @get:Input
    var mappingFileTrimIndents: Boolean = false

    /**
     * datadog-ci.json file, if found or applicable for the particular task.
     */
    @Optional
    @get:InputFile
    var datadogCiFile: File? = null

    /**
     * The sourceSet root folders.
     */
    @get:InputFiles
    var sourceSetRoots: List<File> = emptyList()

    /**
     * The file containing the repository description.
     */
    @get:OutputFile
    var repositoryFile: File = File("")

    init {
        group = "datadog"
        description = "Uploads the Proguard/R8 mapping file to Datadog"
        // it is never up-to-date, because request may fail
        outputs.upToDateWhen { false }
    }

    // region Task

    /**
     * Uploads the mapping file to Datadog.
     */
    @TaskAction
    fun applyTask() {
        datadogCiFile?.let {
            applyDatadogCiConfig(it)
        }
        applySiteFromEnvironment()
        validateConfiguration()

        if (apiKey.contains("\"") || apiKey.contains("'")) {
            throw IllegalStateException(
                "DD-API-KEY provided shouldn't contain quotes or apostrophes."
            )
        }

        var mappingFile = File(mappingFilePath)
        if (!validateMappingFile(mappingFile)) return

        mappingFile = shrinkMappingFile(mappingFile)

        val repositories = repositoryDetector.detectRepositories(
            sourceSetRoots,
            remoteRepositoryUrl
        )
        if (repositories.isNotEmpty()) {
            generateRepositoryFile(repositories)
        }

        val site = DatadogSite.valueOf(site)
        uploader.upload(
            site,
            mappingFile,
            if (repositories.isEmpty()) null else repositoryFile,
            apiKey,
            DdAppIdentifier(
                serviceName = serviceName,
                version = versionName,
                variant = variantName
            ),
            repositories.firstOrNull()
        )
    }

    private fun applySiteFromEnvironment() {
        val environmentSite = System.getenv(DATADOG_SITE)
        if (!environmentSite.isNullOrEmpty()) {
            if (this.site.isNotEmpty()) {
                LOGGER.info(
                    "Site property found as DATADOG_SITE env variable, but it will be ignored," +
                        " because also an explicit one was provided in extension."
                )
                return
            }
            val site = DatadogSite.fromDomain(environmentSite)
            if (site == null) {
                LOGGER.warn("Unknown Datadog domain provided: $environmentSite, ignoring it.")
            } else {
                LOGGER.info("Site property found in Datadog CI config file, using it.")
                this.site = site.name
            }
        }
    }

    private fun applyDatadogCiConfig(datadogCiFile: File) {
        try {
            val config = JSONObject(datadogCiFile.readText())
            applyApiKeyFromDatadogCiConfig(config)
            applySiteFromDatadogCiConfig(config)
        } catch (e: JSONException) {
            LOGGER.error("Failed to parse Datadog CI config file.", e)
        }
    }

    private fun applyApiKeyFromDatadogCiConfig(config: JSONObject) {
        val apiKey = config.optString(DATADOG_CI_API_KEY_PROPERTY, null)
        if (!apiKey.isNullOrEmpty()) {
            if (this.apiKeySource == ApiKeySource.GRADLE_PROPERTY) {
                LOGGER.info(
                    "API key found in Datadog CI config file, but it will be ignored," +
                        " because also an explicit one was provided as a gradle property."
                )
            } else {
                LOGGER.info("API key found in Datadog CI config file, using it.")
                this.apiKey = apiKey
                this.apiKeySource = ApiKeySource.DATADOG_CI_CONFIG_FILE
            }
        }
    }

    private fun applySiteFromDatadogCiConfig(config: JSONObject) {
        if (this.site.isNotEmpty()) {
            LOGGER.info(
                "Site property found in Datadog CI config file, but it will be ignored," +
                    " because also an explicit one was provided in extension."
            )
            return
        }
        val siteAsDomain = config.optString(DATADOG_CI_SITE_PROPERTY, null)
        if (!siteAsDomain.isNullOrEmpty()) {
            val site = DatadogSite.fromDomain(siteAsDomain)
            if (site == null) {
                LOGGER.warn("Unknown Datadog domain provided: $siteAsDomain, ignoring it.")
            } else {
                LOGGER.info("Site property found in Datadog CI config file, using it.")
                this.site = site.name
            }
        }
    }

    // endregion

    // region Internal

    @Suppress("CheckInternal")
    private fun validateConfiguration() {
        check(apiKey.isNotBlank()) {
            "Make sure you define an API KEY to upload your mapping files to Datadog. " +
                "Create a DD_API_KEY or DATADOG_API_KEY environment variable, gradle" +
                " property or define it in datadog-ci.json file."
        }

        if (site.isBlank()) {
            site = DatadogSite.US1.name
        } else {
            val validSiteIds = DatadogSite.validIds
            check(site in validSiteIds) {
                "You need to provide a valid site (one of ${validSiteIds.joinToString()})"
            }
        }
    }

    private fun generateRepositoryFile(repositories: List<RepositoryInfo>) {
        val data = JSONArray()
        repositories.forEach {
            data.put(it.toJson())
            LOGGER.info("Detected repository:\n${it.toJson().toString(4)}")
        }

        val jsonObject = JSONObject()
        jsonObject.put("version", 1)
        jsonObject.put("data", data)

        repositoryFile.parentFile.mkdirs()
        repositoryFile.writeText(jsonObject.toString(0))
    }

    @Suppress("CheckInternal")
    private fun validateMappingFile(mappingFile: File): Boolean {
        if (!mappingFile.exists()) {
            println("There's no mapping file $mappingFilePath, nothing to upload")
            return false
        }

        check(mappingFile.isFile) { "Expected $mappingFilePath to be a file" }

        check(mappingFile.canRead()) { "Cannot read file $mappingFilePath" }

        return true
    }

    private fun shrinkMappingFile(
        mappingFile: File
    ): File {
        if (!mappingFileTrimIndents && mappingFilePackagesAliases.isEmpty()) return mappingFile

        LOGGER.info(
            "Size of ${mappingFile.path} before optimization" +
                " is ${mappingFile.length()} bytes"
        )

        val shrinkedFile = File(mappingFile.parent, MAPPING_OPTIMIZED_FILE_NAME)
        // sort is needed to have predictable replacement in the following case:
        // imagine there are 2 keys - "androidx.work" and "androidx.work.Job", and the latter
        // occurs much more often than the rest under "androidx.work.*". So for the more efficient
        // compression we need first to process the replacement of "androidx.work.Job" and only
        // after that any possible prefix (which has a smaller length).
        val replacements = mappingFilePackagesAliases.entries
            .sortedByDescending { it.key.length }
            .map {
                Regex("(?<=^|\\W)${it.key}(?=\\W)") to it.value
            }

        mappingFile.readLines()
            .run {
                if (mappingFileTrimIndents) map { it.trimStart() } else this
            }
            .map {
                applyShortAliases(it, replacements)
            }
            .forEach {
                shrinkedFile.appendText(it + "\n")
            }

        LOGGER.info("Size of optimized file is ${shrinkedFile.length()} bytes")

        return shrinkedFile
    }

    private fun applyShortAliases(
        line: String,
        replacements: List<Pair<Regex, String>>
    ): String {
        val isLineWithRename = !line.startsWith(MAPPING_FILE_COMMENT_CHAR) &&
            line.contains(MAPPING_FILE_CHANGE_DELIMITER)

        return if (isLineWithRename) {
            val parts = line.split(MAPPING_FILE_CHANGE_DELIMITER)
            if (parts.size != 2) {
                LOGGER.info(
                    "Unexpected number of '$MAPPING_FILE_CHANGE_DELIMITER'" +
                        " (${parts.size - 1}) in the obfuscation line."
                )
                line
            } else {
                val (source, obfuscated) = parts
                val aliasedSource =
                    replacements.fold(source) { state, entry ->
                        val (from, to) = entry
                        state.replace(from, to)
                    }
                "$aliasedSource$MAPPING_FILE_CHANGE_DELIMITER$obfuscated"
            }
        } else {
            line
        }
    }

    // endregion

    internal companion object {
        private const val MAPPING_FILE_CHANGE_DELIMITER = "->"
        private const val MAPPING_FILE_COMMENT_CHAR = '#'
        const val MAPPING_OPTIMIZED_FILE_NAME = "mapping-shrinked.txt"

        private const val DATADOG_CI_API_KEY_PROPERTY = "apiKey"
        private const val DATADOG_CI_SITE_PROPERTY = "datadogSite"
        const val DATADOG_SITE = "DATADOG_SITE"
    }
}
