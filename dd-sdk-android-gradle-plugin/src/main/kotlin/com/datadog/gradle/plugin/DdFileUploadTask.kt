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
import javax.inject.Inject
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * A Gradle task to upload symbolication files to Datadog servers (symbol files, proguard files, etc.).
 */
abstract class DdFilesUploadTask @Inject constructor(
    providerFactory: ProviderFactory,
): DefaultTask() {
    @get:Internal
    internal var uploader: Uploader = OkHttpUploader()

    /**
     * The API key to use for uploading.
     */
    @get:Input
    var apiKey: String = ""

    /**
     * Whether to disable GZIP compression for the upload.
     */
    private val disableGzipOption: Provider<String> =
        providerFactory.gradleProperty(DdMappingFileUploadTask.DISABLE_GZIP_GRADLE_PROPERTY)

    /**
     * Source of the API key set: environment, gradle property, etc.
     */
    @get:Input
    var apiKeySource: ApiKeySource = ApiKeySource.NONE

    @get:Internal
    var repositoryDetector: RepositoryDetector? = null

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
     * The version code of the application.
     */
    @get:Input
    var versionCode: Int = 0

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

    // TODO: IEnumerable?
    @Internal
    internal abstract fun getFilesList(): List<Uploader.UploadFileInfo>

    @TaskAction
    fun applyTask() {
        datadogCiFile?.let {
            applyDatadogCiConfig(it)
        }
        applySiteFromEnvironment()
        validateConfiguration()

        check(!(apiKey.contains("\"") || apiKey.contains("'"))) {
            DdMappingFileUploadTask.INVALID_API_KEY_FORMAT_ERROR
        }

        check(buildId.isPresent && buildId.get().isNotEmpty()) {
            DdMappingFileUploadTask.MISSING_BUILD_ID_ERROR
        }

        val mappingFiles = getFilesList()
        if (mappingFiles.isEmpty()) return

        var repositories = emptyList<RepositoryInfo>()
        repositoryDetector?.let {
            repositories = it.detectRepositories(
                sourceSetRoots,
                remoteRepositoryUrl
            )
        }
        if (repositories.isNotEmpty()) {
            generateRepositoryFile(repositories)
        }

        val site = DatadogSite.valueOf(site)
        for (mappingFile in mappingFiles) {
            LOGGER.info("Uploading ${mappingFile.fileType} file: ${mappingFile.file.absolutePath}")
            uploader.upload(
                site,
                mappingFile,
                if (repositories.isEmpty()) null else repositoryFile,
                apiKey,
                DdAppIdentifier(
                    serviceName = serviceName,
                    version = versionName,
                    versionCode = versionCode,
                    variant = variantName,
                    buildId = buildId.get()
                ),
                repositories.firstOrNull(),
                !disableGzipOption.isPresent
            )
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

        versionName = variant.versionName ?: variant.versionName
        versionCode = variant.versionCode
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
        if (this.site.isNotEmpty()) {
            DdAndroidGradlePlugin.LOGGER.info(
                "Site property found in Datadog CI config file, but it will be ignored," +
                        " because also an explicit one was provided in extension."
            )
            return
        }
        val siteAsDomain = config.optString(DATADOG_CI_SITE_PROPERTY, null)
        if (!siteAsDomain.isNullOrEmpty()) {
            val site = DatadogSite.fromDomain(siteAsDomain)
            if (site == null) {
                DdAndroidGradlePlugin.LOGGER.warn("Unknown Datadog domain provided: $siteAsDomain, ignoring it.")
            } else {
                DdAndroidGradlePlugin.LOGGER.info("Site property found in Datadog CI config file, using it.")
                this.site = site.name
            }
        }
    }

    @Suppress("CheckInternal")
    private fun validateConfiguration() {
        check(apiKey.isNotBlank()) { DdMappingFileUploadTask.API_KEY_MISSING_ERROR }

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
            DdAndroidGradlePlugin.LOGGER.info("Detected repository:\n${it.toJson().toString(
                INDENT
            )}")
        }

        val jsonObject = JSONObject()
        jsonObject.put("version", 1)
        jsonObject.put("data", data)

        repositoryFile.parentFile.mkdirs()
        repositoryFile.writeText(jsonObject.toString(0))
    }

    internal companion object {
        private const val INDENT = 4

        private const val DATADOG_CI_API_KEY_PROPERTY = "apiKey"
        private const val DATADOG_CI_SITE_PROPERTY = "datadogSite"
        const val DATADOG_SITE = "DATADOG_SITE"

        internal val LOGGER = Logging.getLogger("DdFileUploadTask")
    }
}