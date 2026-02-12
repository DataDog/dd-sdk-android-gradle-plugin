/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.utils.capitalizeChar
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

internal class DdAndroidGradlePluginConfigureVariantForUploadTest : DdAndroidGradlePluginTestBase() {

    @Test
    fun `M configure the upload task with the variant info W configureVariantForUploadTask()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.versionName = null
        fakeExtension.serviceName = null
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn
            "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            mockBuildIdGenerationTask(fakeBuildId),
            fakeProject.providers.provider { fakeApiKey },
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey.get()).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource.get()).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName.get()).isEqualTo(versionName)
        assertThat(task.serviceName.get()).isEqualTo(packageName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(task.mappingFile.get().asFile)
            .isEqualTo(File(fakeProject.projectDir, fakeExtension.mappingFilePath))
        assertThat(task.mappingFilePackagesAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases)
        assertThat(task.datadogCiFile).isNull()
        assertThat(task.buildId.get()).isEqualTo(fakeBuildId)
    }

    @Test
    fun `M configure the upload task with the extension info W configureVariantForUploadTask()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            mockBuildIdGenerationTask(fakeBuildId),
            fakeProject.providers.provider { fakeApiKey },
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey.get()).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource.get()).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName.get()).isEqualTo(fakeExtension.versionName)
        assertThat(task.serviceName.get()).isEqualTo(fakeExtension.serviceName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(task.mappingFile.get().asFile)
            .isEqualTo(File(fakeProject.projectDir, fakeExtension.mappingFilePath))
        assertThat(task.mappingFilePackagesAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases)
        assertThat(task.mappingFileTrimIndents)
            .isEqualTo(fakeExtension.mappingFileTrimIndents)
        assertThat(task.datadogCiFile).isNull()
        assertThat(task.buildId.get()).isEqualTo(fakeBuildId)
    }

    @Test
    fun `M configure the upload task with sanitized mapping aliases W configureVariantForUploadTask()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String,
        forge: Forge
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        val aliasToFilterOut =
            packageName.substring(0, forge.anInt(min = 1, max = packageName.length + 1)) to
                forge.anAlphabeticalString()

        fakeExtension.mappingFilePackageAliases = mutableMapOf<String, String>().apply {
            putAll(fakeExtension.mappingFilePackageAliases)
            plus(aliasToFilterOut)
        }

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            mockBuildIdGenerationTask(fakeBuildId),
            fakeProject.providers.provider { fakeApiKey },
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey.get()).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource.get()).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName.get()).isEqualTo(fakeExtension.versionName)
        assertThat(task.serviceName.get()).isEqualTo(fakeExtension.serviceName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(task.mappingFile.get().asFile)
            .isEqualTo(File(fakeProject.projectDir, fakeExtension.mappingFilePath))
        assertThat(task.mappingFilePackagesAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases.minus(aliasToFilterOut.first))
        assertThat(task.mappingFileTrimIndents)
            .isEqualTo(fakeExtension.mappingFileTrimIndents)
        assertThat(task.datadogCiFile).isNull()
        assertThat(task.buildId.get()).isEqualTo(fakeBuildId)
    }

    @Test
    fun `M use sensible defaults W configureVariantForUploadTask() { empty config }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.serviceName = null
        fakeExtension.versionName = null
        fakeExtension.site = null
        fakeExtension.remoteRepositoryUrl = null
        fakeExtension.mappingFilePath = null
        fakeExtension.mappingFilePackageAliases = emptyMap()
        fakeExtension.mappingFileTrimIndents = false
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        val fakeMappingFilePath = "${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.mappingFile) doReturn fakeMappingFilePath.asFileProvider()
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            mockBuildIdGenerationTask(fakeBuildId),
            fakeProject.providers.provider { fakeApiKey },
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey.get()).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource.get()).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName.get()).isEqualTo(versionName)
        assertThat(task.serviceName.get()).isEqualTo(packageName)
        assertThat(task.remoteRepositoryUrl).isEmpty()
        assertThat(task.site).isEqualTo("")
        assertThat(task.mappingFile.get().asFile.path).isEqualTo(fakeMappingFilePath)
        assertThat(task.mappingFilePackagesAliases).isEmpty()
        assertThat(task.mappingFileTrimIndents).isFalse
        assertThat(task.datadogCiFile).isNull()
        assertThat(task.buildId.get()).isEqualTo(fakeBuildId)
    }

    @Test
    fun `M apply datadog CI file W configureVariantForUploadTask()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.serviceName = null
        fakeExtension.versionName = null
        fakeExtension.site = null
        fakeExtension.remoteRepositoryUrl = null
        fakeExtension.mappingFilePath = null
        fakeExtension.mappingFilePackageAliases = emptyMap()
        fakeExtension.mappingFileTrimIndents = false
        fakeExtension.ignoreDatadogCiFileConfig = false
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        val fakeMappingFilePath = "${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.mappingFile) doReturn fakeMappingFilePath.asFileProvider()
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        val fakeDatadogCiFile = File(fakeProject.projectDir, "datadog-ci.json")
        fakeDatadogCiFile.createNewFile()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            mockBuildIdGenerationTask(fakeBuildId),
            fakeProject.providers.provider { fakeApiKey },
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey.get()).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource.get()).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName.get()).isEqualTo(versionName)
        assertThat(task.serviceName.get()).isEqualTo(packageName)
        assertThat(task.remoteRepositoryUrl).isEmpty()
        assertThat(task.site).isEqualTo("")
        assertThat(task.mappingFile.get().asFile.path).isEqualTo(fakeMappingFilePath)
        assertThat(task.mappingFilePackagesAliases).isEmpty()
        assertThat(task.mappingFileTrimIndents).isFalse
        assertThat(task.datadogCiFile).isEqualTo(fakeDatadogCiFile)
        assertThat(task.buildId.get()).isEqualTo(fakeBuildId)
    }

    @Test
    fun `M not apply datadog CI file W configureVariantForUploadTask() { ignoreDatadogCiFileConfig }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.serviceName = null
        fakeExtension.versionName = null
        fakeExtension.site = null
        fakeExtension.remoteRepositoryUrl = null
        fakeExtension.mappingFilePath = null
        fakeExtension.mappingFilePackageAliases = emptyMap()
        fakeExtension.mappingFileTrimIndents = false
        fakeExtension.ignoreDatadogCiFileConfig = true
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        val fakeMappingFilePath = "${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.mappingFile) doReturn fakeMappingFilePath.asFileProvider()
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        val fakeDatadogCiFile = File(fakeProject.projectDir, "datadog-ci.json")
        fakeDatadogCiFile.createNewFile()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            mockBuildIdGenerationTask(fakeBuildId),
            fakeProject.providers.provider { fakeApiKey },
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey.get()).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource.get()).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName.get()).isEqualTo(versionName)
        assertThat(task.serviceName.get()).isEqualTo(packageName)
        assertThat(task.remoteRepositoryUrl).isEmpty()
        assertThat(task.site).isEqualTo("")
        assertThat(task.mappingFile.get().asFile.path).isEqualTo(fakeMappingFilePath)
        assertThat(task.mappingFilePackagesAliases).isEmpty()
        assertThat(task.mappingFileTrimIndents).isFalse
        assertThat(task.datadogCiFile).isNull()
        assertThat(task.buildId.get()).isEqualTo(fakeBuildId)
    }

    @Test
    fun `M not create buildId task W configureTasksForVariant() { no deobfuscation, no native build enabled }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.isMinifyEnabled) doReturn false
        whenever(mockVariant.isNativeBuildEnabled) doReturn false
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName

        // When
        testedPlugin.configureTasksForVariant(
            fakeProject,
            fakeExtension,
            mockVariant,
            fakeProject.providers.provider { fakeApiKey }
        )

        // Then
        val allTasks = fakeProject.tasks.map { it.name }
        assertThat(allTasks).doesNotContain("generateBuildId${variantName.replaceFirstChar { capitalizeChar(it) }}")
        verify(mockVariant, never()).bindWith(any<TaskProvider<GenerateBuildIdTask>>(), any<Provider<Directory>>())
    }

    @Test
    fun `M create uploadNdkSymbolFiles task W configureTasksForVariant() { native build providers }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @IntForgery(min = 0) versionCode: Int,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.isNativeBuildEnabled) doReturn true
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        // When
        testedPlugin.configureTasksForVariant(
            fakeProject,
            fakeExtension,
            mockVariant,
            fakeProject.providers.provider { fakeApiKey }
        )

        // Then
        val allTasks = fakeProject.tasks.map { it.name }
        assertThat(allTasks).contains("uploadNdkSymbolFiles${variantName.replaceFirstChar { capitalizeChar(it) }}")
        verify(mockVariant).bindWith(any<NdkSymbolFileUploadTask>())
    }

    @Test
    fun `M not create uploadNdkSymbolFiles task W configureTasksForVariant() { no native build providers }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @IntForgery(min = 0) versionCode: Int,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.isMinifyEnabled) doReturn true
        whenever(mockVariant.isNativeBuildEnabled) doReturn false
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        // When
        testedPlugin.configureTasksForVariant(
            fakeProject,
            fakeExtension,
            mockVariant,
            fakeProject.providers.provider { fakeApiKey }
        )

        // Then
        val allTasks = fakeProject.tasks.map { it.name }
        assertThat(allTasks).allMatch { !it.startsWith("uploadNdkSymbolFiles") }
        verify(mockVariant, never()).bindWith(any<NdkSymbolFileUploadTask>())
    }

    @Test
    fun `M not create mapping upload task W configureTasksForVariant() { no deobfuscation }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @IntForgery(min = 0) versionCode: Int,
        @StringForgery versionName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.isMinifyEnabled) doReturn false
        whenever(mockVariant.isNativeBuildEnabled) doReturn true
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        // When
        testedPlugin.configureTasksForVariant(
            fakeProject,
            fakeExtension,
            mockVariant,
            fakeProject.providers.provider { fakeApiKey }
        )

        // Then
        val allTasks = fakeProject.tasks.map { it.name }
        assertThat(allTasks).allMatch { !it.startsWith(DdAndroidGradlePlugin.UPLOAD_TASK_NAME) }
    }

    @Test
    fun `M configure the upload task W configureVariantForUploadTask() { non default obfuscation }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.nonDefaultObfuscation = true
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.isMinifyEnabled) doReturn false
        whenever(mockVariant.collectJavaAndKotlinSourceDirectories()) doReturn emptyList<File>().asProvider()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            mockBuildIdGenerationTask(fakeBuildId),
            fakeProject.providers.provider { fakeApiKey },
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey.get()).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource.get()).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName.get()).isEqualTo(fakeExtension.versionName)
        assertThat(task.serviceName.get()).isEqualTo(fakeExtension.serviceName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(task.mappingFile.get().asFile)
            .isEqualTo(File(fakeProject.projectDir, fakeExtension.mappingFilePath))
        assertThat(task.mappingFilePackagesAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases)
        assertThat(task.mappingFileTrimIndents)
            .isEqualTo(fakeExtension.mappingFileTrimIndents)
        assertThat(task.datadogCiFile).isNull()
        assertThat(task.buildId.get()).isEqualTo(fakeBuildId)
    }
}
