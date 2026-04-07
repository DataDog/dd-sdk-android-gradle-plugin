/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.DdAppIdentifier
import com.datadog.gradle.plugin.internal.OkHttpUploader
import com.datadog.gradle.plugin.internal.Uploader
import com.datadog.gradle.plugin.internal.variant.AppVariant
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * A Gradle task to upload symbolication files to Datadog servers (NDK symbol files,
 * Proguard/R8 files, etc.).
 */
abstract class FileUploadTask @Inject constructor(
    providerFactory: ProviderFactory,
    @get:Internal internal val repositoryDetector: RepositoryDetector
) : DefaultTask() {

    @get:Internal
    internal var uploader: Uploader = OkHttpUploader()

    /**
     * The API key to use for uploading.
     */
    @get:Input
    abstract val apiKey: Property<String>

    /**
     * Source of the API key set: environment, Gradle property, etc.
     */
    @get:Input
    abstract val apiKeySource: Property<ApiKeySource>

    private val disableGzipOption: Provider<String> =
        providerFactory.gradleProperty(DISABLE_GZIP_GRADLE_PROPERTY)

    // needed for functional tests, because we don't have real API key
    private val emulateNetworkCall: Provider<String> =
        providerFactory.gradleProperty(EMULATE_UPLOAD_NETWORK_CALL)

    /**
     * The variant name of the application.
     */
    @get:Input
    abstract val variantName: Property<String>

    /**
     * The version name of the application.
     */
    @get:Input
    abstract val versionName: Property<String>

    /**
     * The version code of the application. Need to be a provider, because resolution during
     * configuration phase may cause incompatibility with other plugins if legacy Variant API is used.
     */
    @get:Input
    abstract val versionCode: Property<Int>

    /**
     * The service name of the application (by default, it is your app's package name).
     */
    @get:Input
    abstract val serviceName: Property<String>

    /**
     * The Datadog site to upload to (one of "US1", "EU1", "US1_FED").
     */
    @get:Input
    abstract val site: Property<String>

    /**
     * The url of the remote repository where the source code was deployed.
     */
    @get:Input
    abstract val remoteRepositoryUrl: Property<String>

    /**
     * File containing the build ID used for mapping file matching.
     *
     * We intentionally use InputFiles instead of InputFile, because the build ID file may be
     * absent during configuration for standalone upload invocations. The task validates this
     * condition itself at execution time to keep the plugin-specific error message.
     */
    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val buildIdFile: RegularFileProperty

    /**
     * The sourceSet root folders.
     */
    @get:InputFiles
    abstract val sourceSetRoots: ListProperty<File>

    /**
     * The file containing the repository description.
     */
    @get:OutputFile
    abstract val repositoryFile: RegularFileProperty

    init {
        group = DdAndroidGradlePlugin.DATADOG_TASK_GROUP
        // it is never up-to-date, because request may fail
        outputs.upToDateWhen { false }
    }

    /**
     * Uploads the files retrieved from `getFilesList` to Datadog.
     */
    @TaskAction
    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun applyTask() {
        validateConfiguration()

        check(!(apiKey.get().contains("\"") || apiKey.get().contains("'"))) {
            INVALID_API_KEY_FORMAT_ERROR
        }

        val buildId = readBuildId()
        check(buildId.isNotEmpty()) {
            MISSING_BUILD_ID_ERROR
        }

        val mappingFiles = getFilesList()
        if (mappingFiles.isEmpty()) {
            LOGGER.warn("No mapping files to upload.")
            return
        }

        // it can be an overlap between java and kotlin directories and since File doesn't override
        // equals for set comparison, we will remove duplicates manually
        val uniqueSourceSetRoots = sourceSetRoots.get()
            .map { it.absolutePath }
            .distinct()
            .map { File(it) }

        val repositories = repositoryDetector.detectRepositories(
            uniqueSourceSetRoots,
            remoteRepositoryUrl.get()
        )

        if (repositories.isNotEmpty()) {
            generateRepositoryFile(repositories)
        }

        val site = DatadogSite.valueOf(site.get())
        val caughtErrors = mutableListOf<Exception>()

        for (mappingFile in mappingFiles) {
            LOGGER.info("Uploading ${mappingFile.fileType} file: ${mappingFile.file.absolutePath}")
            try {
                uploader.upload(
                    site,
                    mappingFile,
                    if (repositories.isEmpty()) null else repositoryFile.get().asFile,
                    apiKey.get(),
                    DdAppIdentifier(
                        serviceName = serviceName.get(),
                        version = versionName.get(),
                        versionCode = versionCode.get(),
                        variant = variantName.get(),
                        buildId = buildId
                    ),
                    repositories.firstOrNull(),
                    !disableGzipOption.isPresent,
                    emulateNetworkCall.isPresent
                )
            } catch (e: Exception) {
                caughtErrors.add(e)
            }
        }
        // If any errors occurred, throw them as a single exception
        if (caughtErrors.isNotEmpty()) {
            if (caughtErrors.count() == 1) {
                throw caughtErrors.first()
            } else {
                val consolidatedError = RuntimeException("Multiple errors occurred during upload")
                caughtErrors.forEach {
                    consolidatedError.addSuppressed(it)
                }
                throw consolidatedError
            }
        }
    }

    // region Internal

    @Internal
    internal abstract fun getFilesList(): List<Uploader.UploadFileInfo>

    internal fun configureWith(
        apiKeyProvider: Provider<ApiKey>,
        extensionConfiguration: DdExtensionConfiguration,
        variant: AppVariant
    ) {
        apiKey.set(apiKeyProvider.map { it.value })
        apiKeySource.set(apiKeyProvider.map { it.source })

        versionName.set(variant.versionName)
        versionCode.set(variant.versionCode)

        if (extensionConfiguration.serviceName != null) {
            serviceName.set(extensionConfiguration.serviceName)
        } else {
            serviceName.set(variant.applicationId)
        }

        variantName.set(variant.flavorName)
        remoteRepositoryUrl.set(extensionConfiguration.remoteRepositoryUrl.orEmpty())
    }

    // endregion

    // region Private

    private fun readBuildId(): String {
        val file = buildIdFile.orNull?.asFile ?: return ""
        return if (file.exists()) {
            file.readText().trim()
        } else {
            ""
        }
    }

    @Suppress("CheckInternal")
    private fun validateConfiguration() {
        check(apiKey.get().isNotBlank()) { API_KEY_MISSING_ERROR }
        val validSiteIds = DatadogSite.validIds
        check(site.get() in validSiteIds) {
            "You need to provide a valid site (one of ${validSiteIds.joinToString()})"
        }
    }

    private fun generateRepositoryFile(repositories: List<RepositoryInfo>) {
        val data = JSONArray()
        repositories.forEach {
            data.put(it.toJson())
            DdAndroidGradlePlugin.LOGGER.info(
                "Detected repository:\n${it.toJson().toString(
                    INDENT
                )}"
            )
        }

        val jsonObject = JSONObject()
        jsonObject.put("version", REPOSITORY_FILE_VERSION)
        jsonObject.put("data", data)

        repositoryFile.get().asFile.let {
            it.parentFile.mkdirs()
            it.writeText(jsonObject.toString(0))
        }
    }

    // endregion

    internal companion object {
        private const val REPOSITORY_FILE_VERSION = 1
        private const val INDENT = 4

        internal val LOGGER = Logging.getLogger("DdFileUploadTask")

        const val DISABLE_GZIP_GRADLE_PROPERTY = "dd-disable-gzip"
        const val EMULATE_UPLOAD_NETWORK_CALL = "dd-emulate-upload-call"

        const val API_KEY_MISSING_ERROR = "Make sure you define an API KEY to upload your mapping files to Datadog. " +
            "Create a DD_API_KEY or DATADOG_API_KEY environment variable, gradle" +
            " property or define it in datadog-ci.json file."
        const val INVALID_API_KEY_FORMAT_ERROR =
            "DD_API_KEY provided shouldn't contain quotes or apostrophes."
        const val MISSING_BUILD_ID_ERROR =
            "Build ID is missing, you need to run upload task only after APK/AAB file is generated."
    }
}
