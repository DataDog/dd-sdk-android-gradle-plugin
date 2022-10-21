/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.internal.VariantIterator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.process.ExecOperations
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

/**
 * Plugin adding tasks for Android projects using Datadog's SDK for Android.
 */
class DdAndroidGradlePlugin @Inject constructor(
    private val execOps: ExecOperations
) : Plugin<Project> {

    // region Plugin

    /** @inheritdoc */
    override fun apply(target: Project) {
        val extension = target.extensions.create(EXT_NAME, DdExtension::class.java)
        val apiKey = resolveApiKey(target)

        target.afterEvaluate {
            val androidExtension = target.extensions.findByType(AppExtension::class.java)
            if (androidExtension == null) {
                LOGGER.error(ERROR_NOT_ANDROID)
            } else {
                androidExtension.applicationVariants.all { variant ->
                    configureVariantForUploadTask(target, variant, apiKey, extension)
                    configureVariantForSdkCheck(target, variant, extension)
                }
            }
        }
    }

    // endregion

    // region Internal

    // TODO RUMM-2382 use ProviderFactory/Provider APIs to watch changes in external environment
    internal fun resolveApiKey(target: Project): ApiKey {
        val propertyKey = target.findProperty(DD_API_KEY)?.toString()
        if (!propertyKey.isNullOrBlank()) return ApiKey(propertyKey, ApiKeySource.GRADLE_PROPERTY)

        val environmentKey = System.getenv(DD_API_KEY)
        if (!environmentKey.isNullOrBlank()) return ApiKey(environmentKey, ApiKeySource.ENVIRONMENT)

        val alternativeEnvironmentKey = System.getenv(DATADOG_API_KEY)
        if (!alternativeEnvironmentKey.isNullOrBlank()) {
            return ApiKey(alternativeEnvironmentKey, ApiKeySource.ENVIRONMENT)
        }
        return ApiKey.NONE
    }

    @Suppress("DefaultLocale")
    internal fun configureVariantForUploadTask(
        target: Project,
        variant: ApplicationVariant,
        apiKey: ApiKey,
        extension: DdExtension
    ): Task? {
        if (!variant.buildType.isMinifyEnabled) {
            LOGGER.warn("Minifying disabled for variant ${variant.name}, no upload task created")
            return null
        }

        if (!extension.enabled) {
            LOGGER.warn("Extension disabled for variant ${variant.name}, no upload task created")
            return null
        }

        val flavorName = variant.flavorName
        val uploadTaskName = UPLOAD_TASK_NAME + variant.name.capitalize()
        // TODO RUMM-2382 use tasks.register
        val uploadTask = target.tasks.create(
            uploadTaskName,
            DdMappingFileUploadTask::class.java,
            GitRepositoryDetector(execOps)
        )
        val extensionConfiguration = resolveExtensionConfiguration(extension, variant)

        configureVariantTask(uploadTask, apiKey, flavorName, extensionConfiguration, variant)

        val outputsDir = File(target.buildDir, "outputs")
        uploadTask.mappingFilePath =
            resolveMappingFilePath(extensionConfiguration, outputsDir, variant)
        uploadTask.mappingFilePackagesAliases =
            filterMappingFileReplacements(
                extensionConfiguration.mappingFilePackageAliases,
                variant.applicationId
            )
        uploadTask.mappingFileTrimIndents = extensionConfiguration.mappingFileTrimIndents
        if (!extensionConfiguration.ignoreDatadogCiFileConfig) {
            uploadTask.datadogCiFile = findDatadogCiFile(target.projectDir)
        }

        val reportsDir = File(outputsDir, "reports")
        val datadogDir = File(reportsDir, "datadog")
        uploadTask.repositoryFile = File(datadogDir, "repository.json")

        val roots = mutableListOf<File>()
        variant.sourceSets.forEach {
            roots.addAll(it.javaDirectories)
        }
        uploadTask.sourceSetRoots = roots

        return uploadTask
    }

    @Suppress("DefaultLocale")
    internal fun configureVariantForSdkCheck(
        target: Project,
        variant: ApplicationVariant,
        extension: DdExtension
    ): Provider<DdCheckSdkDepsTask>? {
        if (!extension.enabled) {
            LOGGER.warn("Extension disabled for variant ${variant.name}, no sdk check task created")
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
        outputsDir: File,
        variant: ApplicationVariant
    ): String {
        val customPath = extensionConfiguration.mappingFilePath
        return if (customPath != null) {
            customPath
        } else {
            val mappingDir = File(outputsDir, "mapping")
            val flavorDir = File(mappingDir, variant.name)
            File(flavorDir, "mapping.txt").path
        }
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

    // endregion

    companion object {

        internal const val DD_API_KEY = "DD_API_KEY"

        internal const val DATADOG_API_KEY = "DATADOG_API_KEY"

        internal val LOGGER = LoggerFactory.getLogger("DdAndroidGradlePlugin")

        private const val EXT_NAME = "datadog"

        private const val UPLOAD_TASK_NAME = "uploadMapping"

        private const val ERROR_NOT_ANDROID = "The dd-android-gradle-plugin has been applied on " +
            "a non android application project"

        private const val MAX_DATADOG_CI_FILE_LOOKUP_LEVELS = 4
    }
}
