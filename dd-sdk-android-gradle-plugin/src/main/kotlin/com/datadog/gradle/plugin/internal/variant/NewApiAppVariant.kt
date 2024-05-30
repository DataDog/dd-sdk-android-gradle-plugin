/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.variant

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.VariantOutput
import com.android.build.gradle.tasks.ExternalNativeBuildTask
import com.datadog.gradle.plugin.GenerateBuildIdTask
import com.datadog.gradle.plugin.MappingFileUploadTask
import com.datadog.gradle.plugin.NdkSymbolFileUploadTask
import com.datadog.gradle.plugin.internal.getSearchObjDirs
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal class NewApiAppVariant(
    private val variant: ApplicationVariant,
    private val target: Project
) : AppVariant {

    private val providerFactory = target.providers

    override val name: String
        get() = variant.name
    override val applicationId: Provider<String>
        get() = variant.applicationId
    override val flavorName: String
        get() = variant.flavorName.orEmpty()
    override val versionCode: Provider<Int>
        get() = providerFactory.provider { variant.mainOutput?.versionCode?.orNull ?: 1 }
    override val versionName: Provider<String>
        get() = providerFactory.provider { variant.mainOutput?.versionName?.orNull.orEmpty() }
    override val compileConfiguration: Configuration
        get() = variant.compileConfiguration
    override val isNativeBuildEnabled: Boolean
        get() = variant.externalNativeBuild != null

    @Suppress("UnstableApiUsage")
    override val isMinifyEnabled: Boolean
        get() = variant.isMinifyEnabled
    override val buildTypeName: String
        get() = variant.buildType.orEmpty()
    override val flavors: List<String>
        // dimension to flavor name
        get() = variant.productFlavors.map { it.second }
    override val mappingFile: Provider<RegularFile>
        get() = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)

    override fun collectJavaAndKotlinSourceDirectories(): Provider<List<File>> {
        val allJava = variant.sources.java?.all

        @Suppress("UnstableApiUsage")
        val allKotlin = variant.sources.kotlin?.all
        return if (allJava != null) {
            if (allKotlin != null) {
                allJava.zip(allKotlin) { java, kotlin -> java + kotlin }.asFileCollectionProvider()
            } else {
                allJava.asFileCollectionProvider()
            }
        } else {
            allKotlin?.asFileCollectionProvider() ?: providerFactory.provider { emptyList() }
        }
    }

    override fun bindWith(ndkUploadTask: NdkSymbolFileUploadTask) {
        target.tasks.withType(ExternalNativeBuildTask::class.java).forEach {
            val searchFiles = it.getSearchObjDirs(providerFactory)
            ndkUploadTask.searchDirectories.from(searchFiles)
            ndkUploadTask.dependsOn(it)
        }
    }

    override fun bindWith(mappingFileUploadTask: MappingFileUploadTask) {
        // nothing is needed, dependency on minification task is created by mapping file provider
    }

    override fun bindWith(
        generateBuildIdTask: TaskProvider<GenerateBuildIdTask>,
        buildIdDirectory: Provider<Directory>
    ) {
        variant.sources.assets?.addGeneratedSourceDirectory(generateBuildIdTask) {
            it.buildIdDirectory
        }
    }

    // region Private

    private fun Provider<out Collection<Directory>>.asFileCollectionProvider() =
        map { collection -> collection.map { it.asFile } }

    // may not be precise, but we need this info only for metadata anyway
    private val ApplicationVariant.mainOutput: VariantOutput?
        get() = outputs.firstOrNull { it.enabled.get() && it.versionCode.orNull != null }

    // endregion
}
