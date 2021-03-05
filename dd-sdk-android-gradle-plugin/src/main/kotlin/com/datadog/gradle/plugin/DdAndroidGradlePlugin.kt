/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.internal.MissingSdkException
import com.datadog.gradle.plugin.internal.VariantIterator
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedDependency
import org.slf4j.LoggerFactory

/**
 * Plugin adding tasks for Android projects using Datadog's SDK for Android.
 */
class DdAndroidGradlePlugin : Plugin<Project> {

    // region Plugin

    /** @inheritdoc */
    override fun apply(target: Project) {
        val androidExtension = target.extensions.findByType(AppExtension::class.java)
        if (androidExtension == null) {
            LOGGER.error(ERROR_NOT_ANDROID)
            return
        }

        val extension = target.extensions.create(EXT_NAME, DdExtension::class.java)
        extension.variants = target.container(DdExtensionConfiguration::class.java)
        val apiKey = resolveApiKey(target)

        target.afterEvaluate {
            androidExtension.applicationVariants.forEach { variant ->
                configureVariantForUploadTask(target, variant, apiKey, extension)
                configureVariantForSdkCheck(target, variant, extension)
            }
        }
    }

    // endregion

    // region Internal

    internal fun resolveApiKey(target: Project): String {
        val propertyKey = target.findProperty(DD_API_KEY)?.toString()
        if (!propertyKey.isNullOrBlank()) return propertyKey

        val environmentKey = System.getenv(DD_API_KEY)
        if (!environmentKey.isNullOrBlank()) return environmentKey

        return ""
    }

    @Suppress("DefaultLocale")
    internal fun configureVariantForUploadTask(
        target: Project,
        variant: ApplicationVariant,
        apiKey: String,
        extension: DdExtension
    ): Task? {
        if (!variant.buildType.isMinifyEnabled) {
            // Proguard/R8 are not enabled, let's not create a task
            return null
        }

        if (!extension.enabled) {
            // lets not create any task
            return null
        }

        val flavorName = variant.flavorName
        val uploadTaskName = UPLOAD_TASK_NAME + variant.name.capitalize()
        val uploadTask = target.tasks.create(
            uploadTaskName,
            DdMappingFileUploadTask::class.java,
            GitRepositoryDetector()
        )
        val extensionConfiguration = resolveExtensionConfiguration(extension, variant)

        configureVariantTask(uploadTask, apiKey, flavorName, extensionConfiguration, variant)

        val outputsDir = File(target.buildDir, "outputs")
        val mappingDir = File(outputsDir, "mapping")
        val flavorDir = File(mappingDir, variant.name)
        uploadTask.mappingFilePath = File(flavorDir, "mapping.txt").path

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
    ): Task? {

        if (!extension.enabled) {
            return null
        }

        // variants will have name like proDebug, but compile task will have a name like
        // compileProDebugSources. It can be other tasks like compileProDebugAndroidTestSources
        // or compileProDebugUnitTestSources, but we are not interested in these. This is fragile
        // and depends on the AGP naming convention
        val compileTask = target.tasks
            .findByName("compile${variant.name.capitalize()}Sources")

        if (compileTask == null) {
            LOGGER.warn(
                "Cannot find compilation task for the ${variant.name} variant, please" +
                    " report the issue at" +
                    " https://github.com/DataDog/dd-sdk-android-gradle-plugin/issues"
            )
            return null
        } else {
            // this will postpone the check until the actual compilation for the particular variant.
            // by doing this we are avoiding pulling all the configurations for all the variants
            // after build script is evaluated, which may be a heavy task for the big projects
            return compileTask.doFirst {
                val firstLevelModuleDependencies = variant.runtimeConfiguration
                    .resolvedConfiguration
                    .firstLevelModuleDependencies

                if (!isDatadogDependencyPresent(firstLevelModuleDependencies)) {

                    val extensionConfiguration = resolveExtensionConfiguration(
                        extension,
                        variant
                    )

                    val sdkCheckLevel = extensionConfiguration.checkProjectDependencies
                        ?: SdkCheckLevel.FAIL

                    when (sdkCheckLevel) {
                        SdkCheckLevel.FAIL -> {
                            throw MissingSdkException(
                                MISSING_DD_SDK_MESSAGE.format(variant.name)
                            )
                        }
                        SdkCheckLevel.WARN -> {
                            LOGGER.warn(MISSING_DD_SDK_MESSAGE.format(variant.name))
                        }
                        SdkCheckLevel.NONE -> {
                            // no-op, ignore that and make compiler happy
                        }
                    }
                }
            }
        }
    }

    private fun configureVariantTask(
        uploadTask: DdMappingFileUploadTask,
        apiKey: String,
        flavorName: String,
        extensionConfiguration: DdExtensionConfiguration,
        variant: ApplicationVariant
    ) {
        uploadTask.apiKey = apiKey
        uploadTask.variantName = flavorName

        uploadTask.site = extensionConfiguration.site ?: ""
        uploadTask.versionName = extensionConfiguration.versionName ?: variant.versionName
        uploadTask.serviceName = extensionConfiguration.serviceName ?: variant.applicationId
    }

    internal fun resolveExtensionConfiguration(
        extension: DdExtension,
        variant: ApplicationVariant
    ): DdExtensionConfiguration {
        val configuration = DdExtensionConfiguration()
        configuration.updateWith(extension)

        val flavors = variant.productFlavors.map { it.name }
        val iterator = VariantIterator(flavors)
        iterator.forEach {
            val config = extension.variants.findByName(it)
            if (config != null) {
                configuration.updateWith(config)
            }
        }
        return configuration
    }

    internal fun isDatadogDependencyPresent(dependencies: Set<ResolvedDependency>): Boolean {
        return dependencies.any {
            (it.moduleGroup == DD_SDK_GROUP && it.moduleName == DD_SDK_NAME) ||
                isDatadogDependencyPresent(it.children)
        }
    }

    // endregion

    companion object {

        internal const val DD_API_KEY = "DD_API_KEY"

        internal val LOGGER = LoggerFactory.getLogger("DdAndroidGradlePlugin")

        private const val EXT_NAME = "datadog"

        private const val UPLOAD_TASK_NAME = "uploadMapping"

        private const val ERROR_NOT_ANDROID = "The dd-android-gradle-plugin has been applied on " +
            "a non android application project"

        internal const val MISSING_DD_SDK_MESSAGE = "Following application variant doesn't" +
            " have Datadog SDK included: %s"

        private const val DD_SDK_NAME = "dd-sdk-android"

        private const val DD_SDK_GROUP = "com.datadoghq"
    }
}
