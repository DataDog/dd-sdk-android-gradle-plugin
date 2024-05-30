/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.variant

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.datadog.gradle.plugin.DdAndroidGradlePlugin
import com.datadog.gradle.plugin.GenerateBuildIdTask
import com.datadog.gradle.plugin.MappingFileUploadTask
import com.datadog.gradle.plugin.NdkSymbolFileUploadTask
import com.datadog.gradle.plugin.internal.CurrentAgpVersion
import com.datadog.gradle.plugin.internal.getSearchObjDirs
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

internal class LegacyApiAppVariant(
    private val variant: ApplicationVariant,
    private val appExtension: AppExtension,
    private val target: Project
) : AppVariant {

    private val providerFactory = target.providers
    private val projectLayout = target.layout

    override val name: String
        get() = variant.name
    override val applicationId: Provider<String>
        get() = providerFactory.provider { variant.applicationId }
    override val flavorName: String
        get() = variant.flavorName
    override val versionCode: Provider<Int>
        get() = providerFactory.provider { variant.versionCode }
    override val versionName: Provider<String>
        get() = providerFactory.provider { variant.versionName.orEmpty() }
    override val compileConfiguration: Configuration
        get() = variant.compileConfiguration
    override val isNativeBuildEnabled: Boolean
        get() = variant.externalNativeBuildProviders.isNotEmpty()
    override val isMinifyEnabled: Boolean
        get() = variant.buildType.isMinifyEnabled
    override val buildTypeName: String
        get() = variant.buildType.name
    override val flavors: List<String>
        get() = variant.productFlavors.map { it.name }
    override val mappingFile: Provider<RegularFile>
        get() = if (CurrentAgpVersion.CAN_QUERY_MAPPING_FILE_PROVIDER) {
            variant.mappingFileProvider
                .flatMap {
                    providerFactory.provider {
                        try {
                            projectLayout.projectDirectory.file(it.singleFile.absolutePath)
                        } catch (e: IllegalStateException) {
                            DdAndroidGradlePlugin.LOGGER.info(
                                "Mapping FileCollection is empty or contains multiple files",
                                e
                            )
                            null
                        }
                    }.orElse(legacyMappingFileProvider)
                }
        } else {
            legacyMappingFileProvider
        }

    private val legacyMappingFileProvider: Provider<RegularFile>
        get() = projectLayout.buildDirectory.file(legacyMappingFilePath.toString())

    private val legacyMappingFilePath: Path
        get() = Paths.get("outputs", "mapping", variant.name, "mapping.txt")

    override fun collectJavaAndKotlinSourceDirectories(): Provider<List<File>> {
        val roots = mutableListOf<File>()
        variant.sourceSets.forEach {
            roots.addAll(it.javaDirectories)
            if (CurrentAgpVersion.SUPPORTS_KOTLIN_DIRECTORIES_SOURCE_PROVIDER) {
                roots.addAll(it.kotlinDirectories)
            }
        }
        return providerFactory.provider { roots }
    }

    override fun bindWith(ndkUploadTask: NdkSymbolFileUploadTask) {
        val nativeBuildProviders = variant.externalNativeBuildProviders
        nativeBuildProviders.forEach { buildTask ->
            val searchFiles = buildTask.getSearchObjDirs(providerFactory)

            ndkUploadTask.searchDirectories.from(searchFiles)
            ndkUploadTask.dependsOn(buildTask)
        }
    }

    override fun bindWith(mappingFileUploadTask: MappingFileUploadTask) {
        val minifyTask = target.tasks.findByName("minify${variant.name.capitalize()}WithR8") ?: return
        mappingFileUploadTask.dependsOn(minifyTask)
    }

    override fun bindWith(
        generateBuildIdTask: TaskProvider<GenerateBuildIdTask>,
        buildIdDirectory: Provider<Directory>
    ) {
        // we could generate buildIdDirectory inside GenerateBuildIdTask and read it here as
        // property using flatMap, but when Gradle sync is done inside Android Studio there is an error
        // Querying the mapped value of provider (java.util.Set) before task ... has completed is
        // not supported, which doesn't happen when Android Studio is not used (pure Gradle build)
        // so applying such workaround
        appExtension.sourceSets.getByName(variant.name).assets.srcDir(buildIdDirectory)

        val variantName = variant.name.capitalize()
        listOf(
            "package${variantName}Bundle",
            "build${variantName}PreBundle",
            "lintVitalAnalyze$variantName",
            "lintVitalReport$variantName",
            "generate${variantName}LintVitalReportModel"
        ).forEach {
            target.tasks.findByName(it)?.dependsOn(generateBuildIdTask)
        }

        // don't merge these 2 into list to call forEach, because common superclass for them
        // is different between AGP versions, which may cause ClassCastException
        variant.mergeAssetsProvider.configure { it.dependsOn(generateBuildIdTask) }
        variant.packageApplicationProvider.configure { it.dependsOn(generateBuildIdTask) }
    }
}
