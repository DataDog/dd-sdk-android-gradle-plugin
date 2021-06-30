/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.internal.DdAppIdentifier
import com.datadog.gradle.plugin.internal.DdConfiguration
import com.datadog.gradle.plugin.internal.OkHttpUploader
import com.datadog.gradle.plugin.internal.Uploader
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray
import org.json.JSONObject

/**
 * A Gradle task to upload a Proguard/R8 mapping file to Datadog servers.
 */
open class DdMappingFileUploadTask
@Inject constructor(
    @get:Internal internal val repositoryDetector: RepositoryDetector
) : DefaultTask() {

    @get:Internal
    internal var uploader: Uploader = OkHttpUploader()

    /**
     * The API Key used to upload the data.
     */
    @get: Input
    var apiKey: String = ""

    /**
     * The variant name of the application.
     */
    @get:Input
    var variantName: String = ""

    /**
     * The version name of the application.
     */
    @get: Input
    var versionName: String = ""

    /**
     * The service name of the application (by default, it is your app's package name).
     */
    @get: Input
    var serviceName: String = ""

    /**
     * The Datadog site to upload to (one of "US", "EU", "GOV").
     */
    @get: Input
    var site: String = ""

    /**
     * The url of the remote repository where the source code was deployed.
     */
    @get: Input
    var remoteRepositoryUrl: String = ""

    /**
     * The path to the mapping file to upload.
     */
    @get:Input
    var mappingFilePath: String = ""

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
        validateConfiguration()

        val mappingFile = File(mappingFilePath)
        if (!validateMappingFile(mappingFile)) return

        val repositories =
            repositoryDetector.detectRepositories(sourceSetRoots, remoteRepositoryUrl)
        if (repositories.isNotEmpty()) {
            generateRepositoryFile(repositories)
        }

        val configuration = DdConfiguration(
            site = DdConfiguration.Site.valueOf(site),
            apiKey = apiKey
        )
        uploader.upload(
            configuration.buildUrl(),
            mappingFile,
            if (repositories.isEmpty()) null else repositoryFile,
            DdAppIdentifier(
                serviceName = serviceName,
                version = versionName,
                variant = variantName
            ),
            repositories.firstOrNull()
        )
    }

    // endregion

    // region Internal

    @Suppress("CheckInternal")
    private fun validateConfiguration() {
        check(apiKey.isNotBlank()) {
            "Make sure you define an API KEY to upload your mapping files to Datadog. " +
                "Create a DD_API_KEY environment variable or gradle property."
        }

        if (site.isBlank()) {
            site = DdConfiguration.Site.US.name
        } else {
            val validSiteIds = DdConfiguration.Site.validIds
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

    // endregion
}
