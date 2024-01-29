/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.Version
import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.internal.VariantIterator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject
import kotlin.io.path.Path

/**
 * Plugin adding tasks for Android projects using Datadog's SDK for Android.
 */
@Suppress("TooManyFunctions")
class DdAndroidGradlePlugin @Inject constructor(
    private val execOps: ExecOperations,
    private val providerFactory: ProviderFactory
) : Plugin<Project> {

    // region Plugin

    /** @inheritdoc */
    override fun apply(target: Project) {
        val extension = target.extensions.create(EXT_NAME, DdExtension::class.java)
        val apiKey = resolveApiKey(target)

        // need to use withPlugin instead of afterEvaluate, because otherwise generated assets
        // folder with buildId is not picked by AGP by some reason
        target.pluginManager.withPlugin("com.android.application") {
            val androidExtension = target.androidApplicationExtension ?: return@withPlugin
            androidExtension.applicationVariants.all { variant ->
                if (extension.enabled) {
                    configureTasksForVariant(
                        target,
                        androidExtension,
                        extension,
                        variant,
                        apiKey
                    )
                }
            }
        }

        target.afterEvaluate {
            val androidExtension = target.androidApplicationExtension
            if (androidExtension == null) {
                LOGGER.error(ERROR_NOT_ANDROID)
            } else if (!extension.enabled) {
                LOGGER.info("Datadog extension disabled, no upload task created")
            }
        }
    }

    // endregion

    // region Internal

    internal fun configureTasksForVariant(
        target: Project,
        androidExtension: AppExtension,
        datadogExtension: DdExtension,
        variant: ApplicationVariant,
        apiKey: ApiKey
    ) {
        val buildIdGenerationTask =
            configureBuildIdGenerationTask(target, androidExtension, variant)

        if (isObfuscationEnabled(variant, datadogExtension)) {
            configureVariantForUploadTask(
                target,
                variant,
                buildIdGenerationTask,
                apiKey,
                datadogExtension
            )
        } else {
            LOGGER.info("Minifying disabled for variant ${variant.name}, no upload task created")
        }
        configureSymbolUploadTask(
            target,
            androidExtension,
            datadogExtension,
            variant
        )
        configureVariantForSdkCheck(target, variant, datadogExtension)
    }

    @Suppress("ReturnCount")
    // TODO RUMM-2382 use ProviderFactory/Provider APIs to watch changes in external environment
    internal fun resolveApiKey(target: Project): ApiKey {
        val apiKey = listOf(
            ApiKey(target.stringProperty(DD_API_KEY).orEmpty(), ApiKeySource.GRADLE_PROPERTY),
            ApiKey(target.stringProperty(DATADOG_API_KEY).orEmpty(), ApiKeySource.GRADLE_PROPERTY),
            ApiKey(System.getenv(DD_API_KEY).orEmpty(), ApiKeySource.ENVIRONMENT),
            ApiKey(System.getenv(DATADOG_API_KEY).orEmpty(), ApiKeySource.ENVIRONMENT)
        ).firstOrNull { it.value.isNotBlank() }

        return apiKey ?: ApiKey.NONE
    }

    internal fun configureSymbolUploadTask(
        target: Project,
        appException: AppExtension,
        extension: DdExtension,
        variant: ApplicationVariant
    ) :TaskProvider<DdSymbolFileUploadTask>? {
        val apiKey = resolveApiKey(target)
        val extensionConfiguration = resolveExtensionConfiguration(extension, variant)

        val uploadTask = DdSymbolFileUploadTask.register(
            target,
            variant,
            providerFactory,
            apiKey,
            extensionConfiguration,
            GitRepositoryDetector(execOps),
        )

        return uploadTask
    }

    @Suppress("StringLiteralDuplication")
    internal fun configureBuildIdGenerationTask(
        target: Project,
        appExtension: AppExtension,
        variant: ApplicationVariant
    ): TaskProvider<GenerateBuildIdTask> {
        val buildIdDirectory = target.layout.buildDirectory
            .dir(Path("generated", "datadog", "buildId", variant.name).toString())
        val buildIdGenerationTask = GenerateBuildIdTask.register(target, variant, buildIdDirectory)

        // we could generate buildIdDirectory inside GenerateBuildIdTask and read it here as
        // property using flatMap, but when Gradle sync is done inside Android Studio there is an error
        // Querying the mapped value of provider (java.util.Set) before task ... has completed is
        // not supported, which doesn't happen when Android Studio is not used (pure Gradle build)
        // so applying such workaround
        // TODO RUM-0000 use new AndroidComponents API to inject generated stuff, it is more friendly
        appExtension.sourceSets.getByName(variant.name).assets.srcDir(buildIdDirectory)

        val variantName = variant.name.capitalize()
        listOf(
            "package${variantName}Bundle",
            "build${variantName}PreBundle",
            "lintVitalAnalyze$variantName",
            "lintVitalReport$variantName",
            "generate${variantName}LintVitalReportModel"
        ).forEach {
            target.tasks.findByName(it)?.dependsOn(buildIdGenerationTask)
        }

        // don't merge these 2 into list to call forEach, because common superclass for them
        // is different between AGP versions, which may cause ClassCastException
        variant.mergeAssetsProvider.configure { it.dependsOn(buildIdGenerationTask) }
        variant.packageApplicationProvider.configure { it.dependsOn(buildIdGenerationTask) }

        return buildIdGenerationTask
    }

    @Suppress("DefaultLocale", "ReturnCount")
    internal fun configureVariantForUploadTask(
        target: Project,
        variant: ApplicationVariant,
        buildIdGenerationTask: TaskProvider<GenerateBuildIdTask>,
        apiKey: ApiKey,
        extension: DdExtension
    ): Task {
        val extensionConfiguration = resolveExtensionConfiguration(extension, variant)
        val flavorName = variant.flavorName
        val uploadTaskName = UPLOAD_TASK_NAME + variant.name.capitalize()
        // TODO RUMM-2382 use tasks.register
        val uploadTask = target.tasks.create(
            uploadTaskName,
            DdMappingFileUploadTask::class.java,
            GitRepositoryDetector(execOps)
        )

        configureVariantTask(uploadTask, apiKey, flavorName, extensionConfiguration, variant)

        // upload task shouldn't depend on the build ID generation task, but only read its property,
        // because upload task may be triggered after assemble task and we don't want to re-generate
        // build ID, because it will be different then from the one which is already embedded in
        // the application package
        uploadTask.buildId = buildIdGenerationTask.flatMap {
            it.buildIdFile.flatMap {
                providerFactory.provider { it.asFile.readText().trim() }
            }
        }
        uploadTask.mappingFilePath = resolveMappingFilePath(extensionConfiguration, target, variant)
        uploadTask.mappingFilePackagesAliases =
            filterMappingFileReplacements(
                extensionConfiguration.mappingFilePackageAliases,
                variant.applicationId
            )
        uploadTask.mappingFileTrimIndents = extensionConfiguration.mappingFileTrimIndents
        if (!extensionConfiguration.ignoreDatadogCiFileConfig) {
            uploadTask.datadogCiFile = findDatadogCiFile(target.projectDir)
        }

        uploadTask.repositoryFile = resolveDatadogRepositoryFile(target)

        val roots = mutableListOf<File>()
        variant.sourceSets.forEach {
            roots.addAll(it.javaDirectories)
            if (isAgp7OrAbove()) {
                roots.addAll(it.kotlinDirectories)
            }
        }

        // it can be an overlap between java and kotlin directories and since File doesn't override
        // equals for set comparison, we will remove duplicates manually
        uploadTask.sourceSetRoots = roots.map { it.absolutePath }
            .distinct()
            .map { File(it) }

        return uploadTask
    }

    @Suppress("DefaultLocale", "ReturnCount")
    internal fun configureVariantForSdkCheck(
        target: Project,
        variant: ApplicationVariant,
        extension: DdExtension
    ): Provider<DdCheckSdkDepsTask>? {
        if (!extension.enabled) {
            LOGGER.info("Extension disabled for variant ${variant.name}, no sdk check task created")
            return null
        }

        val compileTask = findCompilationTask(target.tasks, variant)

        if (compileTask == null) {
            LOGGER.warn(
                "Cannot find compilation task for the ${variant.name} variant, please" +
                    " report the issue at" +
                    " https://github.com/DataDog/dd-sdk-android-gradle-plugin/issues"
            )
            return null
        } else {
            val extensionConfiguration = resolveExtensionConfiguration(
                extension,
                variant
            )
            if (extensionConfiguration.checkProjectDependencies == SdkCheckLevel.NONE ||
                extensionConfiguration.checkProjectDependencies == null
            ) {
                return null
            }
            val checkDepsTaskName = "checkSdkDeps${variant.name.capitalize()}"
            val resolvedCheckDependencyFlag =
                extensionConfiguration.checkProjectDependencies ?: SdkCheckLevel.FAIL
            val checkDepsTaskProvider = target.tasks.register(
                checkDepsTaskName,
                DdCheckSdkDepsTask::class.java
            ) {
                it.configurationName.set(variant.compileConfiguration.name)
                it.sdkCheckLevel.set(resolvedCheckDependencyFlag)
                it.variantName.set(variant.name)
            }
            compileTask.finalizedBy(checkDepsTaskProvider)
            return checkDepsTaskProvider
        }
    }

    @Suppress("DefaultLocale")
    private fun findCompilationTask(
        taskContainer: TaskContainer,
        appVariant: ApplicationVariant
    ): Task? {
        // variants will have name like proDebug, but compile task will have a name like
        // compileProDebugSources. It can be other tasks like compileProDebugAndroidTestSources
        // or compileProDebugUnitTestSources, but we are not interested in these. This is fragile
        // and depends on the AGP naming convention

        // tricky moment: compileXXXSources exists before AGP 7.1.0, but in AGP 7.1 it is in the
        // container, but doesn't participate in the build process (=> not called). On the other
        // hand compileXXXJavaWithJavac exists on AGP 7.1 and is part of the build process. So we
        // will try first to get newer task and if it is not there, then fallback to the old one.
        return taskContainer.findByName("compile${appVariant.name.capitalize()}JavaWithJavac")
            ?: taskContainer.findByName("compile${appVariant.name.capitalize()}Sources")
    }

    private fun resolveMappingFilePath(
        extensionConfiguration: DdExtensionConfiguration,
        target: Project,
        variant: ApplicationVariant
    ): String {
        val customPath = extensionConfiguration.mappingFilePath
        return if (customPath != null) {
            customPath
        } else {
            val outputsDir = File(target.buildDir, "outputs")
            val mappingDir = File(outputsDir, "mapping")
            val flavorDir = File(mappingDir, variant.name)
            File(flavorDir, "mapping.txt").path
        }
    }

    @Suppress("StringLiteralDuplication")
    private fun resolveDatadogRepositoryFile(target: Project): File {
        val outputsDir = File(target.buildDir, "outputs")
        val reportsDir = File(outputsDir, "reports")
        val datadogDir = File(reportsDir, "datadog")
        return File(datadogDir, "repository.json")
    }

    private fun filterMappingFileReplacements(
        replacements: Map<String, String>,
        applicationId: String
    ): Map<String, String> {
        return replacements.filter {
            // not necessarily applicationId == package attribute from the Manifest, but it is
            // best and cheapest effort to avoid wrong renaming (otherwise we may loose Git
            // integration feature).
            if (applicationId.startsWith(it.key)) {
                LOGGER.warn(
                    "Renaming of package prefix=${it.key} will be skipped, because" +
                        " it belongs to the application package."
                )
                false
            } else {
                true
            }
        }
    }

    private fun configureVariantTask(
        uploadTask: DdMappingFileUploadTask,
        apiKey: ApiKey,
        flavorName: String,
        extensionConfiguration: DdExtensionConfiguration,
        variant: ApplicationVariant
    ) {
        uploadTask.apiKey = apiKey.value
        uploadTask.apiKeySource = apiKey.source
        uploadTask.variantName = flavorName

        uploadTask.site = extensionConfiguration.site ?: ""
        uploadTask.versionName = extensionConfiguration.versionName ?: variant.versionName
        uploadTask.versionCode = variant.versionCode
        uploadTask.serviceName = extensionConfiguration.serviceName ?: variant.applicationId
        uploadTask.remoteRepositoryUrl = extensionConfiguration.remoteRepositoryUrl ?: ""
    }

    internal fun resolveExtensionConfiguration(
        extension: DdExtension,
        variant: ApplicationVariant
    ): DdExtensionConfiguration {
        val configuration = DdExtensionConfiguration()
        configuration.updateWith(extension)

        val flavors = variant.productFlavors.map { it.name }
        val buildType = variant.buildType.name
        val iterator = VariantIterator(flavors + buildType)
        iterator.forEach {
            val config = extension.variants.findByName(it)
            if (config != null) {
                configuration.updateWith(config)
            }
        }
        return configuration
    }

    internal fun findDatadogCiFile(projectDir: File): File? {
        var currentDir: File? = projectDir
        var levelsUp = 0
        while (currentDir != null && levelsUp < MAX_DATADOG_CI_FILE_LOOKUP_LEVELS) {
            val datadogCiFile = File(currentDir, "datadog-ci.json")
            if (datadogCiFile.exists()) {
                return datadogCiFile
            }
            currentDir = currentDir.parentFile
            levelsUp++
        }
        return null
    }

    private fun Project.stringProperty(propertyName: String): String? {
        return findProperty(propertyName)?.toString()
    }

    private fun isObfuscationEnabled(
        variant: ApplicationVariant,
        extension: DdExtension
    ): Boolean {
        val extensionConfiguration = resolveExtensionConfiguration(extension, variant)
        val isDefaultObfuscationEnabled = variant.buildType.isMinifyEnabled
        val isNonDefaultObfuscationEnabled = extensionConfiguration.nonDefaultObfuscation
        return isDefaultObfuscationEnabled || isNonDefaultObfuscationEnabled
    }

    @Suppress("MagicNumber", "ReturnCount")
    private fun isAgp7OrAbove(): Boolean {
        val version = Version.ANDROID_GRADLE_PLUGIN_VERSION
        val groups = version.split(".")
        if (groups.size < 3) return false
        val major = groups[0].toIntOrNull()
        val minor = groups[1].toIntOrNull()
        val patch = groups[2].substringBefore("-").toIntOrNull()
        if (major == null || minor == null || patch == null) return false
        return major >= 7 && minor >= 0 && patch >= 0
    }

    private val Project.androidApplicationExtension: AppExtension?
        get() = extensions.findByType(AppExtension::class.java)

    // endregion

    companion object {

        internal const val DD_API_KEY = "DD_API_KEY"

        internal const val DATADOG_API_KEY = "DATADOG_API_KEY"

        internal const val DATADOG_TASK_GROUP = "datadog"

        internal val LOGGER = Logging.getLogger("DdAndroidGradlePlugin")

        private const val EXT_NAME = "datadog"

        internal const val UPLOAD_TASK_NAME = "uploadMapping"

        private const val ERROR_NOT_ANDROID = "The dd-android-gradle-plugin has been applied on " +
            "a non android application project"

        private const val MAX_DATADOG_CI_FILE_LOOKUP_LEVELS = 4
    }
}
