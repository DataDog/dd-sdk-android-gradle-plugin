/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.Uploader
import com.datadog.gradle.plugin.internal.lazyBuildIdProvider
import com.datadog.gradle.plugin.internal.variant.AppVariant
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskProvider
import java.io.File
import javax.inject.Inject

/**
 * A Gradle task to upload NDK symbol files to Datadog servers.
 */
internal abstract class NdkSymbolFileUploadTask @Inject constructor(
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory,
    repositoryDetector: RepositoryDetector
) : FileUploadTask(providerFactory, repositoryDetector) {

    @get:InputFiles
    val searchDirectories: ConfigurableFileCollection = objectFactory.fileCollection()

    init {
        description =
            "Uploads NDK symbol files to Datadog servers to perform native crash symbolication."
    }

    override fun getFilesList(): List<Uploader.UploadFileInfo> {
        val files = mutableListOf<Uploader.UploadFileInfo>()

        searchDirectories
            .flatMap(this::findSoFiles)
            .toSet()
            .groupBy { it.parentFile }
            .forEach { archToFiles ->
                val arch = archToFiles.key.name
                val archMapping = SUPPORTED_ARCHS.firstOrNull { it.arch == arch }
                if (archMapping == null) {
                    logger.warn(
                        "Unknown architecture: $arch found in folder: ${archToFiles.key.absolutePath}, ignoring it." +
                            " Supported architectures are: " +
                            SUPPORTED_ARCHS.joinToString(", ") { it.arch }
                    )
                    return@forEach
                }
                archToFiles.value.forEach {
                    files.add(
                        Uploader.UploadFileInfo(
                            KEY_NDK_SYMBOL_FILE,
                            it,
                            encoding = ENCODING,
                            TYPE_NDK_SYMBOL_FILE,
                            it.name,
                            mapOf(
                                "arch" to archMapping.uploadArch
                            )
                        )
                    )
                }
            }

        return files
    }

    private fun findSoFiles(searchDirectory: File): Collection<File> {
        return if (searchDirectory.exists() && searchDirectory.isDirectory) {
            searchDirectory.walkTopDown()
                .filter { it.extension == "so" }
                .toSet()
        } else {
            emptySet()
        }
    }

    // Map of Android architecture names to the architecture names recognized by the symbolication service
    data class SupportedArchitectureMapping(
        val arch: String,
        val uploadArch: String
    )

    companion object {
        internal const val TASK_NAME = "uploadNdkSymbolFiles"
        internal const val KEY_NDK_SYMBOL_FILE = "ndk_symbol_file"
        internal const val TYPE_NDK_SYMBOL_FILE = "ndk_symbol_file"
        internal const val ENCODING = "application/octet-stream"
        internal val SUPPORTED_ARCHS = setOf(
            SupportedArchitectureMapping("armeabi-v7a", "arm"),
            SupportedArchitectureMapping("arm64-v8a", "arm64"),
            SupportedArchitectureMapping("x86", "x86"),
            SupportedArchitectureMapping("x86_64", "x64")
        )

        @Suppress("LongParameterList", "ReturnCount")
        fun register(
            project: Project,
            variant: AppVariant,
            buildIdTask: TaskProvider<GenerateBuildIdTask>,
            providerFactory: ProviderFactory,
            apiKeyProvider: Provider<ApiKey>,
            extensionConfiguration: DdExtensionConfiguration,
            repositoryDetector: RepositoryDetector
        ): TaskProvider<NdkSymbolFileUploadTask> {
            return project.tasks.register(
                TASK_NAME + variant.name.capitalize(),
                NdkSymbolFileUploadTask::class.java,
                repositoryDetector
            ).apply {
                configure { task ->
                    task.sourceSetRoots.set(variant.collectJavaAndKotlinSourceDirectories())

                    variant.bindWith(task)

                    task.datadogCiFile = TaskUtils.findDatadogCiFile(project.rootDir)
                    task.repositoryFile = TaskUtils.resolveDatadogRepositoryFile(project)
                    extensionConfiguration.additionalSymbolFilesLocations?.let {
                        task.searchDirectories.from(it.toTypedArray())
                    }
                    task.configureWith(
                        apiKeyProvider,
                        extensionConfiguration,
                        variant
                    )

                    task.buildId.set(buildIdTask.lazyBuildIdProvider(providerFactory))
                }
            }
        }
    }
}
