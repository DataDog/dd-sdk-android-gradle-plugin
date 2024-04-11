package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.DdAppIdentifier
import com.datadog.gradle.plugin.internal.OkHttpUploader
import com.datadog.gradle.plugin.internal.Uploader
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
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
 * A Gradle task to upload symbolication files to Datadog servers (NDK symbol files,
 * Proguard/R8 files, etc.)..
 */
abstract class DdFileUploadTask @Inject constructor(
    private val providerFactory: ProviderFactory,
    @get:Internal internal val repositoryDetector: RepositoryDetector
) : DefaultTask() {
    @get:Internal
    internal var uploader: Uploader = OkHttpUploader()

    /**
     * The API key to use for uploading.
     */
    @get:Input
    var apiKey: String = ""

    private val disableGzipOption: Provider<String> =
        providerFactory.gradleProperty(DdFileUploadTask.DISABLE_GZIP_GRADLE_PROPERTY)

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
     * The version code of the application. Need to be a provider, because resolution during
     * configuration phase may cause incompatibility with other plugins if legacy Variant API is used.
     */
    @get:Input
    var versionCode: Provider<Int> = providerFactory.provider { 0 }

    /**
     * The service name of the application (by default, it is your app's package name).
     */
    @get:Input
    var serviceName: String = ""

    /**
     * The Datadog site to upload to (one of "US1", "EU1", "US1_FED").
     */
    @get:Input
    var site: String = ""

    /**
     * The url of the remote repository where the source code was deployed.
     */
    @get:Input
    var remoteRepositoryUrl: String = ""

    /**
     * Build ID which will be used for mapping file matching.
     */
    @get:Input
    var buildId: Provider<String> = providerFactory.provider { "" }

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
        group = DdAndroidGradlePlugin.DATADOG_TASK_GROUP
        // it is never up-to-date, because request may fail
        outputs.upToDateWhen { false }
    }

    @Internal
    internal abstract fun getFilesList(): List<Uploader.UploadFileInfo>

    /**
     * Uploads the files retrieved from `getFilesList` to Datadog.
     */
    @TaskAction
    @Suppress("TooGenericExceptionCaught")
    fun applyTask() {
        datadogCiFile?.let {
            applyDatadogCiConfig(it)
        }
        applySiteFromEnvironment()
        validateConfiguration()

        check(!(apiKey.contains("\"") || apiKey.contains("'"))) {
            INVALID_API_KEY_FORMAT_ERROR
        }

        check(buildId.isPresent && buildId.get().isNotEmpty()) {
            MISSING_BUILD_ID_ERROR
        }

        val mappingFiles = getFilesList()
        if (mappingFiles.isEmpty()) return

        val repositories = repositoryDetector.detectRepositories(
            sourceSetRoots,
            remoteRepositoryUrl
        )

        if (repositories.isNotEmpty()) {
            generateRepositoryFile(repositories)
        }

        val site = DatadogSite.valueOf(site)
        val caughtErrors = mutableListOf<Exception>()
        for (mappingFile in mappingFiles) {
            LOGGER.info("Uploading ${mappingFile.fileType} file: ${mappingFile.file.absolutePath}")
            try {
                uploader.upload(
                    site,
                    mappingFile,
                    if (repositories.isEmpty()) null else repositoryFile,
                    apiKey,
                    DdAppIdentifier(
                        serviceName = serviceName,
                        version = versionName,
                        versionCode = versionCode.get(),
                        variant = variantName,
                        buildId = buildId.get()
                    ),
                    repositories.firstOrNull(),
                    !disableGzipOption.isPresent
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

    internal fun configureWith(
        apiKey: ApiKey,
        extensionConfiguration: DdExtensionConfiguration,
        variant: ApplicationVariant
    ) {
        this.apiKey = apiKey.value
        apiKeySource = apiKey.source
        site = extensionConfiguration.site ?: ""

        versionName = variant.versionName ?: ""
        versionCode = providerFactory.provider { variant.versionCode }
        serviceName = extensionConfiguration.serviceName ?: variant.applicationId
        variantName = variant.flavorName
        remoteRepositoryUrl = extensionConfiguration.remoteRepositoryUrl ?: ""
    }

    private fun applySiteFromEnvironment() {
        val environmentSite = System.getenv(DATADOG_SITE)
        if (!environmentSite.isNullOrEmpty()) {
            if (this.site.isNotEmpty()) {
                DdAndroidGradlePlugin.LOGGER.info(
                    "Site property found as DATADOG_SITE env variable, but it will be ignored," +
                        " because also an explicit one was provided in extension."
                )
                return
            }
            val site = DatadogSite.fromDomain(environmentSite)
            if (site == null) {
                DdAndroidGradlePlugin.LOGGER.warn("Unknown Datadog domain provided: $environmentSite, ignoring it.")
            } else {
                DdAndroidGradlePlugin.LOGGER.info("Site property found in Datadog CI config file, using it.")
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
            DdAndroidGradlePlugin.LOGGER.error("Failed to parse Datadog CI config file.", e)
        }
    }

    private fun applyApiKeyFromDatadogCiConfig(config: JSONObject) {
        val apiKey = config.optString(DATADOG_CI_API_KEY_PROPERTY, null)
        if (!apiKey.isNullOrEmpty()) {
            if (this.apiKeySource == ApiKeySource.GRADLE_PROPERTY) {
                DdAndroidGradlePlugin.LOGGER.info(
                    "API key found in Datadog CI config file, but it will be ignored," +
                        " because also an explicit one was provided as a gradle property."
                )
            } else {
                DdAndroidGradlePlugin.LOGGER.info("API key found in Datadog CI config file, using it.")
                this.apiKey = apiKey
                this.apiKeySource = ApiKeySource.DATADOG_CI_CONFIG_FILE
            }
        }
    }

    private fun applySiteFromDatadogCiConfig(config: JSONObject) {
        val siteAsDomain = config.optString(DATADOG_CI_SITE_PROPERTY, null)
        if (!siteAsDomain.isNullOrEmpty()) {
            if (this.site.isNotEmpty()) {
                DdAndroidGradlePlugin.LOGGER.info(
                    "Site property found in Datadog CI config file, but it will be ignored," +
                        " because also an explicit one was provided in extension."
                )
            } else {
                val site = DatadogSite.fromDomain(siteAsDomain)
                if (site == null) {
                    DdAndroidGradlePlugin.LOGGER.warn("Unknown Datadog domain provided: $siteAsDomain, ignoring it.")
                } else {
                    DdAndroidGradlePlugin.LOGGER.info("Site property found in Datadog CI config file, using it.")
                    this.site = site.name
                }
            }
        }
    }

    @Suppress("CheckInternal")
    private fun validateConfiguration() {
        check(apiKey.isNotBlank()) { API_KEY_MISSING_ERROR }

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
            DdAndroidGradlePlugin.LOGGER.info(
                "Detected repository:\n${it.toJson().toString(
                    INDENT
                )}"
            )
        }

        val jsonObject = JSONObject()
        jsonObject.put("version", RESPOSITORY_FILE_VERSION)
        jsonObject.put("data", data)

        repositoryFile.parentFile.mkdirs()
        repositoryFile.writeText(jsonObject.toString(0))
    }

    internal companion object {
        private const val RESPOSITORY_FILE_VERSION = 1
        private const val INDENT = 4

        private const val DATADOG_CI_API_KEY_PROPERTY = "apiKey"
        private const val DATADOG_CI_SITE_PROPERTY = "datadogSite"
        const val DATADOG_SITE = "DATADOG_SITE"

        internal val LOGGER = Logging.getLogger("DdFileUploadTask")

        const val DISABLE_GZIP_GRADLE_PROPERTY = "dd-disable-gzip"

        const val API_KEY_MISSING_ERROR = "Make sure you define an API KEY to upload your mapping files to Datadog. " +
            "Create a DD_API_KEY or DATADOG_API_KEY environment variable, gradle" +
            " property or define it in datadog-ci.json file."
        const val INVALID_API_KEY_FORMAT_ERROR =
            "DD_API_KEY provided shouldn't contain quotes or apostrophes."
        const val MISSING_BUILD_ID_ERROR =
            "Build ID is missing, you need to run upload task only after APK/AAB file is generated."
    }
}
