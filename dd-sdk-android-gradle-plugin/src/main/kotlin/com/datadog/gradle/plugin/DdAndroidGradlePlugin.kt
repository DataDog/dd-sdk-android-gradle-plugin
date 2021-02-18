/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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
            System.err.println(ERROR_NOT_ANDROID)
            return
        }

        val extension = target.extensions.create(EXT_NAME, DdExtension::class.java)
        extension.variants = target.container(DdExtensionConfiguration::class.java)
        val apiKey = resolveApiKey(target)

        target.afterEvaluate {
            androidExtension.applicationVariants.forEach {
                configureVariant(target, it, apiKey, extension)
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
    internal fun configureVariant(
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
        val uploadTask = target.tasks.create(uploadTaskName, DdMappingFileUploadTask::class.java)
        val extensionConfiguration = resolveExtensionConfiguration(extension, flavorName)

        configureVariantTask(uploadTask, apiKey, flavorName, extensionConfiguration, variant)

        val outputsDir = File(target.buildDir, "outputs")
        val mappingDir = File(outputsDir, "mapping")
        val flavorDir = File(mappingDir, variant.name)
        uploadTask.mappingFilePath = File(flavorDir, "mapping.txt").path

        return uploadTask
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

        uploadTask.envName = extensionConfiguration.environmentName ?: ""
        uploadTask.site = extensionConfiguration.site ?: ""
        uploadTask.versionName = extensionConfiguration.versionName ?: variant.versionName
        uploadTask.serviceName = extensionConfiguration.serviceName ?: variant.applicationId
    }

    internal fun resolveExtensionConfiguration(
        extension: DdExtension,
        flavorName: String
    ): DdExtensionConfiguration {
        val flavorConfig = extension.variants?.findByName(flavorName)

        return DdExtensionConfiguration().apply {
            environmentName = flavorConfig?.environmentName ?: extension.environmentName
            versionName = flavorConfig?.versionName ?: extension.versionName
            serviceName = flavorConfig?.serviceName ?: extension.serviceName
            site = flavorConfig?.site ?: extension.site
        }
    }

    // endregion

    companion object {

        internal const val DD_API_KEY = "DD_API_KEY"

        internal val LOGGER = LoggerFactory.getLogger("DdAndroidGradlePlugin")

        private const val SUFFIX_DEBUG = "Debug"
        private const val SUFFIX_RELEASE = "Release"

        private const val EXT_NAME = "datadog"

        private const val UPLOAD_TASK_NAME = "uploadMapping"

        private const val ERROR_NOT_ANDROID = "The dd-android-gradle-plugin has been applied on " +
            "a non android application project"
    }
}
