/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.CurrentAgpVersion
import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.internal.VariantIterator
import com.datadog.gradle.plugin.internal.lazyBuildIdProvider
import com.datadog.gradle.plugin.internal.variant.AppVariant
import com.datadog.gradle.plugin.internal.variant.NewApiAppVariant
import com.datadog.gradle.plugin.kcp.DatadogKotlinCompilerPluginCommandLineProcessor.Companion.KOTLIN_COMPILER_PLUGIN_ID
import com.datadog.gradle.plugin.kcp.KotlinCompilerPluginOptions.RECORD_IMAGES
import com.datadog.gradle.plugin.kcp.KotlinCompilerPluginOptions.TRACK_ACTIONS
import com.datadog.gradle.plugin.kcp.KotlinCompilerPluginOptions.TRACK_VIEWS
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.net.URISyntaxException
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

        target.pluginManager.withPlugin("org.jetbrains.kotlin.android") {
            configureKotlinCompilerPlugin(target, extension)
        }

        // need to use withPlugin instead of afterEvaluate, because otherwise generated assets
        // folder with buildId is not picked by AGP by some reason
        target.pluginManager.withPlugin("com.android.application") {
            if (CurrentAgpVersion.CAN_ENABLE_NEW_VARIANT_API && !target.hasProperty(DD_FORCE_LEGACY_VARIANT_API)
            ) {
                val androidComponentsExtension = target.androidApplicationComponentExtension ?: return@withPlugin
                androidComponentsExtension.onVariants { variant ->
                    configureTasksForVariant(
                        target,
                        extension,
                        AppVariant.create(variant, target),
                        apiKey
                    )
                }
            } else {
                val androidExtension = target.androidApplicationExtension ?: return@withPlugin
                androidExtension.applicationVariants.all { variant ->
                    if (extension.enabled) {
                        configureTasksForVariant(
                            target,
                            extension,
                            AppVariant.create(variant, androidExtension, target),
                            apiKey
                        )
                    }
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
        datadogExtension: DdExtension,
        variant: AppVariant,
        apiKey: ApiKey
    ) {
        val isObfuscationEnabled = isObfuscationEnabled(variant, datadogExtension)
        val isNativeBuildRequired = variant.isNativeBuildEnabled
        val isNativeSymbolsTaskRequired =
            isNativeBuildRequired || datadogExtension.additionalSymbolFilesLocations?.isNotEmpty() == true

        if (isObfuscationEnabled || isNativeBuildRequired || isNativeSymbolsTaskRequired) {
            val buildIdGenerationTask =
                configureBuildIdGenerationTask(target, variant)

            if (isObfuscationEnabled) {
                configureVariantForUploadTask(
                    target,
                    variant,
                    buildIdGenerationTask,
                    apiKey,
                    datadogExtension
                )
            } else {
                LOGGER.info("Minifying disabled for variant ${variant.name}, no mapping file upload task created")
            }

            if (isNativeSymbolsTaskRequired) {
                configureNdkSymbolUploadTask(
                    target,
                    datadogExtension,
                    variant,
                    buildIdGenerationTask,
                    apiKey
                )
            } else {
                LOGGER.info(
                    "No native build tasks found for variant ${variant.name}," +
                        " no additionalSymbolFilesLocations provided," +
                        " no NDK symbol file upload task created."
                )
            }
        }

        if (variant is NewApiAppVariant) {
            // need to run this in afterEvaluate, because with new Variant API tasks won't be created yet at this point
            target.afterEvaluate {
                configureVariantForSdkCheck(target, variant, datadogExtension)
            }
        } else {
            configureVariantForSdkCheck(target, variant, datadogExtension)
        }
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

    private fun configureNdkSymbolUploadTask(
        target: Project,
        extension: DdExtension,
        variant: AppVariant,
        buildIdTask: TaskProvider<GenerateBuildIdTask>,
        apiKey: ApiKey
    ): TaskProvider<NdkSymbolFileUploadTask> {
        val extensionConfiguration = resolveExtensionConfiguration(extension, variant)

        val uploadTask = NdkSymbolFileUploadTask.register(
            target,
            variant,
            buildIdTask,
            providerFactory,
            apiKey,
            extensionConfiguration,
            GitRepositoryDetector(execOps)
        )

        return uploadTask
    }

    @Suppress("StringLiteralDuplication")
    private fun configureBuildIdGenerationTask(
        target: Project,
        variant: AppVariant
    ): TaskProvider<GenerateBuildIdTask> {
        val buildIdDirectory = target.layout.buildDirectory
            .dir(Path("generated", "datadog", "buildId", variant.name).toString())
        val buildIdGenerationTask = GenerateBuildIdTask.register(target, variant, buildIdDirectory)

        return buildIdGenerationTask
    }

    @Suppress("DefaultLocale", "ReturnCount")
    internal fun configureVariantForUploadTask(
        target: Project,
        variant: AppVariant,
        buildIdGenerationTask: TaskProvider<GenerateBuildIdTask>,
        apiKey: ApiKey,
        extension: DdExtension
    ): TaskProvider<MappingFileUploadTask> {
        val uploadTaskName = UPLOAD_TASK_NAME + variant.name.capitalize()
        val uploadTask = target.tasks.register(
            uploadTaskName,
            MappingFileUploadTask::class.java,
            GitRepositoryDetector(execOps)
        ).apply {
            configure { uploadTask ->
                @Suppress("MagicNumber")
                if (TaskUtils.isGradleAbove(target, 7, 5)) {
                    uploadTask.notCompatibleWithConfigurationCache(
                        "Datadog Upload Mapping task is not" +
                            " compatible with configuration cache yet."
                    )
                }
                val extensionConfiguration = resolveExtensionConfiguration(extension, variant)
                configureVariantTask(
                    target.objects,
                    uploadTask,
                    apiKey,
                    extensionConfiguration,
                    variant
                )

                uploadTask.buildId.set(buildIdGenerationTask.lazyBuildIdProvider(providerFactory))
                uploadTask.mappingFilePackagesAliases = extensionConfiguration.mappingFilePackageAliases
                uploadTask.mappingFileTrimIndents = extensionConfiguration.mappingFileTrimIndents
                if (!extensionConfiguration.ignoreDatadogCiFileConfig) {
                    uploadTask.datadogCiFile = TaskUtils.findDatadogCiFile(target.projectDir)
                }

                uploadTask.repositoryFile = TaskUtils.resolveDatadogRepositoryFile(target)
            }
        }

        return uploadTask
    }

    @Suppress("ReturnCount")
    internal fun configureVariantForSdkCheck(
        target: Project,
        variant: AppVariant,
        extension: DdExtension
    ): TaskProvider<CheckSdkDepsTask>? {
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
                CheckSdkDepsTask::class.java
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
        appVariant: AppVariant
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

    private fun resolveMappingFile(
        extensionConfiguration: DdExtensionConfiguration,
        objectFactory: ObjectFactory,
        variant: AppVariant
    ): Provider<RegularFile> {
        val customPath = extensionConfiguration.mappingFilePath
        return if (customPath != null) {
            objectFactory.fileProperty().fileValue(File(customPath))
        } else {
            variant.mappingFile
        }
    }

    private fun configureVariantTask(
        objectFactory: ObjectFactory,
        uploadTask: MappingFileUploadTask,
        apiKey: ApiKey,
        extensionConfiguration: DdExtensionConfiguration,
        variant: AppVariant
    ) {
        uploadTask.apiKey = apiKey.value
        uploadTask.apiKeySource = apiKey.source
        uploadTask.variantName = variant.flavorName

        uploadTask.applicationId.set(variant.applicationId)

        uploadTask.mappingFile.set(resolveMappingFile(extensionConfiguration, objectFactory, variant))
        uploadTask.sourceSetRoots.set(variant.collectJavaAndKotlinSourceDirectories())

        uploadTask.site = extensionConfiguration.site ?: ""
        if (extensionConfiguration.versionName != null) {
            uploadTask.versionName.set(extensionConfiguration.versionName)
        } else {
            uploadTask.versionName.set(variant.versionName)
        }
        uploadTask.versionCode.set(variant.versionCode)
        if (extensionConfiguration.serviceName != null) {
            uploadTask.serviceName.set(extensionConfiguration.serviceName)
        } else {
            uploadTask.serviceName.set(variant.applicationId)
        }
        uploadTask.remoteRepositoryUrl = extensionConfiguration.remoteRepositoryUrl ?: ""

        variant.bindWith(uploadTask)
    }

    internal fun resolveExtensionConfiguration(
        extension: DdExtension,
        variant: AppVariant
    ): DdExtensionConfiguration {
        val configuration = DdExtensionConfiguration()
        configuration.updateWith(extension)

        val flavors = variant.flavors
        val buildType = variant.buildTypeName
        val iterator = VariantIterator(flavors + buildType)
        iterator.forEach {
            val config = extension.variants.findByName(it)
            if (config != null) {
                configuration.updateWith(config)
            }
        }
        return configuration
    }

    @Suppress("ReturnCount")
    private fun configureKotlinCompilerPlugin(project: Project, ddExtension: DdExtension) {
        val pluginJarPath = try {
            val codeSource = this::class.java.protectionDomain?.codeSource
            codeSource?.location?.toURI()?.path
        } catch (e: URISyntaxException) {
            LOGGER.error(
                "Can not parse Datadog Gradle Plugin path because the URI is not correctly formatted.",
                e
            )
            return
        } catch (e: SecurityException) {
            LOGGER.error(
                "Failed to access Datadog Gradle Plugin protection domain due to insufficient permissions.",
                e
            )
            return
        }

        if (pluginJarPath == null) {
            LOGGER.warn(
                "$DD_PLUGIN_MAVEN_COORDINATES not found in classpath, " +
                    "Skipping Kotlin Compiler Plugin configuration."
            )
            return
        }
        val composeInstrumentation = ddExtension.composeInstrumentation
        project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
            task.kotlinOptions.freeCompilerArgs += listOf(
                "-Xplugin=$pluginJarPath",
                "-P",
                "plugin:$KOTLIN_COMPILER_PLUGIN_ID:$TRACK_VIEWS=${composeInstrumentation.trackViews.name}",
                "-P",
                "plugin:$KOTLIN_COMPILER_PLUGIN_ID:$TRACK_ACTIONS=${composeInstrumentation.trackActions.name}",
                "-P",
                "plugin:$KOTLIN_COMPILER_PLUGIN_ID:$RECORD_IMAGES=${composeInstrumentation.recordImages.name}"
            )
        }
    }

    private fun Project.stringProperty(propertyName: String): String? {
        return findProperty(propertyName)?.toString()
    }

    private fun isObfuscationEnabled(
        variant: AppVariant,
        extension: DdExtension
    ): Boolean {
        val extensionConfiguration = resolveExtensionConfiguration(extension, variant)
        val isDefaultObfuscationEnabled = variant.isMinifyEnabled
        val isNonDefaultObfuscationEnabled = extensionConfiguration.nonDefaultObfuscation
        return isDefaultObfuscationEnabled || isNonDefaultObfuscationEnabled
    }

    private val Project.androidApplicationExtension: AppExtension?
        get() = extensions.findByType(AppExtension::class.java)

    private val Project.androidApplicationComponentExtension: ApplicationAndroidComponentsExtension?
        get() = extensions.findByType(ApplicationAndroidComponentsExtension::class.java)

    // endregion

    companion object {

        private const val DD_FORCE_LEGACY_VARIANT_API = "dd-force-legacy-variant-api"

        private const val DD_PLUGIN_MAVEN_COORDINATES = "com.datadoghq:dd-sdk-android-gradle-plugin"

        internal const val DD_API_KEY = "DD_API_KEY"

        internal const val DATADOG_API_KEY = "DATADOG_API_KEY"

        internal const val DATADOG_TASK_GROUP = "datadog"

        internal val LOGGER = Logging.getLogger("DdAndroidGradlePlugin")

        private const val EXT_NAME = "datadog"

        internal const val UPLOAD_TASK_NAME = "uploadMapping"

        private const val ERROR_NOT_ANDROID = "The dd-android-gradle-plugin has been applied on " +
            "a non android application project"
    }
}
