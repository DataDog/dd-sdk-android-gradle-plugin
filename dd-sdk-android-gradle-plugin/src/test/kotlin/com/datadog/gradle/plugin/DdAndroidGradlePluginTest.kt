/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.internal.variant.AppVariant
import com.datadog.gradle.plugin.utils.capitalizeChar
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.AdvancedForgery
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.MapForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File
import java.util.UUID

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class DdAndroidGradlePluginTest {

    lateinit var testedPlugin: DdAndroidGradlePlugin

    lateinit var fakeProject: Project

    @Mock
    lateinit var mockVariant: AppVariant

    lateinit var fakeBuildId: String

    @Forgery
    lateinit var fakeExtension: DdExtension

    lateinit var fakeApiKey: ApiKey

    @StringForgery(case = Case.LOWER)
    lateinit var fakeFlavorNames: List<String>

    @StringForgery(regex = "debug|preRelease|release")
    lateinit var fakeBuildTypeName: String

    @BeforeEach
    fun `set up`(forge: Forge) {
        fakeApiKey = ApiKey(
            value = forge.anHexadecimalString(),
            source = forge.aValueFrom(ApiKeySource::class.java)
        )
        fakeFlavorNames = fakeFlavorNames.take(5) // A D F G A♭ A A♭ G F
        fakeBuildId = forge.getForgery<UUID>().toString()
        fakeProject = ProjectBuilder.builder().build()
        testedPlugin = DdAndroidGradlePlugin(
            execOps = mock(),
            providerFactory = fakeProject.providers
        )

        setEnv(DdAndroidGradlePlugin.DD_API_KEY, "")
        setEnv(DdAndroidGradlePlugin.DATADOG_API_KEY, "")
    }

    // region configureVariantForUploadTask()

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
            fakeApiKey,
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
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
            fakeApiKey,
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
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
            fakeApiKey,
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
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
            fakeApiKey,
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
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
            fakeApiKey,
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
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
            fakeApiKey,
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
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
            fakeApiKey
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
            fakeApiKey
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
            fakeApiKey
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
            fakeApiKey
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
            fakeApiKey,
            fakeExtension
        ).get()

        // Then
        check(task is MappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
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

    // endregion

    // region resolveApiKey

    @Test
    fun `M resolve API KEY from project properties W resolveApiKey() { as DD_API_KEY }`() {
        // Given
        fakeProject = mock()
        whenever(fakeProject.findProperty(DdAndroidGradlePlugin.DD_API_KEY)) doReturn
            fakeApiKey.value

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey.value).isEqualTo(fakeApiKey.value)
        assertThat(apiKey.source).isEqualTo(ApiKeySource.GRADLE_PROPERTY)
    }

    @Test
    fun `M resolve API KEY from environment variable W resolveApiKey() { as DD_API_KEY }`() {
        // Given
        setEnv(DdAndroidGradlePlugin.DD_API_KEY, fakeApiKey.value)

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey.value).isEqualTo(fakeApiKey.value)
        assertThat(apiKey.source).isEqualTo(ApiKeySource.ENVIRONMENT)
    }

    @Test
    fun `M resolve API KEY from project properties W resolveApiKey() { as DATADOG_API_KEY }`() {
        // Given
        fakeProject = mock()
        whenever(fakeProject.findProperty(DdAndroidGradlePlugin.DATADOG_API_KEY)) doReturn
            fakeApiKey.value

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey.value).isEqualTo(fakeApiKey.value)
        assertThat(apiKey.source).isEqualTo(ApiKeySource.GRADLE_PROPERTY)
    }

    @Test
    fun `M resolve API KEY from environment variable W resolveApiKey() { as DATADOG_API_KEY }`() {
        // Given
        setEnv(DdAndroidGradlePlugin.DATADOG_API_KEY, fakeApiKey.value)

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey.value).isEqualTo(fakeApiKey.value)
        assertThat(apiKey.source).isEqualTo(ApiKeySource.ENVIRONMENT)
    }

    @Test
    fun `M returns empty String W resolveApiKey() {key not defined anywhere}`() {
        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey).isEqualTo(ApiKey.NONE)
    }

    // endregion

    // region resolveExtensionConfiguration

    @Test
    fun `M return default config W resolveExtensionConfiguration() {no variant config}`() {
        // Given
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(fakeExtension.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(fakeExtension.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return config W resolveExtensionConfiguration() { variant w full config }`(
        @Forgery variantConfig: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(fakeExtension.variants.findByName(variantName)) doReturn variantConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfig.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfig.serviceName)
        assertThat(config.site).isEqualTo(variantConfig.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(variantConfig.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfig.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(variantConfig.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(variantConfig.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(variantConfig.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(variantConfig.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w version only }`(
        @StringForgery versionName: String
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.versionName = versionName
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w service only }`(
        @StringForgery serviceName: String
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.serviceName = serviceName
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w mappingPath }`(
        @StringForgery(regex = "/([a-z]+)/([a-z]+)/([a-z]+)/mapping.txt") mappingFilePath: String
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.mappingFilePath = mappingFilePath
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.mappingFilePath).isEqualTo(mappingFilePath)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return config W resolveExtensionConfiguration() { variant+mappingFilePackageAliases }`(
        @MapForgery(
            key = AdvancedForgery(
                string = [StringForgery(regex = "[a-z]{3}(\\.[a-z]{5,10}){2,4}")]
            ),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ALPHABETICAL)])
        ) mappingFilePackageAliases: Map<String, String>
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.mappingFilePackageAliases = mappingFilePackageAliases
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return config W resolveExtensionConfiguration() { variant+mappingFileTrimIndents }`(
        @BoolForgery mappingFileTrimIndents: Boolean
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.mappingFileTrimIndents = mappingFileTrimIndents
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEmpty()
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w site only }`(
        @Forgery site: DatadogSite
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.site = site.name
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(site.name)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(
            fakeExtension.checkProjectDependencies
        )
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w sdkCheck only }`(
        @Forgery sdkCheckLevel: SdkCheckLevel
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.checkProjectDependencies = sdkCheckLevel
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(sdkCheckLevel)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w remoteUrl only }`(
        @Forgery fakeConfig: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.remoteRepositoryUrl = fakeConfig.remoteRepositoryUrl
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(incompleteConfig.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(
            fakeExtension.checkProjectDependencies
        )
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant + ignoreDdConfig }`(
        @Forgery fakeConfig: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.ignoreDatadogCiFileConfig = fakeConfig.ignoreDatadogCiFileConfig
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(
            fakeExtension.checkProjectDependencies
        )
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(incompleteConfig.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { simple variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigA: DdExtensionConfiguration,
        @Forgery variantConfigB: DdExtensionConfiguration,
        @Forgery variantConfigC: DdExtensionConfiguration
    ) {
        // Given
        val flavorNames = listOf(flavorA, flavorB, flavorC)
        variantConfigA.apply {
            versionName = null
            checkProjectDependencies = null
        }
        variantConfigB.apply {
            serviceName = null
            checkProjectDependencies = null
        }
        variantConfigC.apply { site = null }
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn flavorNames
        whenever(fakeExtension.variants.findByName(flavorA)) doReturn variantConfigA
        whenever(fakeExtension.variants.findByName(flavorB)) doReturn variantConfigB
        whenever(fakeExtension.variants.findByName(flavorC)) doReturn variantConfigC

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfigB.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfigA.serviceName)
        assertThat(config.site).isEqualTo(variantConfigA.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfigC.checkProjectDependencies)
        assertThat(config.remoteRepositoryUrl).isEqualTo(variantConfigA.remoteRepositoryUrl)
        assertThat(config.mappingFilePath).isEqualTo(variantConfigA.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(variantConfigA.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(variantConfigA.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(variantConfigA.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { complex variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigAB: DdExtensionConfiguration,
        @Forgery variantConfigAC: DdExtensionConfiguration,
        @Forgery variantConfigBC: DdExtensionConfiguration
    ) {
        // Given
        val flavorNames = listOf(flavorA, flavorB, flavorC)
        variantConfigAB.apply { versionName = null }
        variantConfigAC.apply { serviceName = null }
        variantConfigBC.apply { site = null }
        variantConfigBC.apply { checkProjectDependencies = null }
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn flavorNames
        whenever(
            fakeExtension.variants.findByName(
                flavorA + flavorB.replaceFirstChar { capitalizeChar(it) }
            )
        )
            .doReturn(variantConfigAB)
        whenever(
            fakeExtension.variants.findByName(
                flavorA + flavorC.replaceFirstChar { capitalizeChar(it) }
            )
        )
            .doReturn(variantConfigAC)
        whenever(
            fakeExtension.variants.findByName(
                flavorB + flavorC.replaceFirstChar { capitalizeChar(it) }
            )
        )
            .doReturn(variantConfigBC)

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfigAC.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfigAB.serviceName)
        assertThat(config.site).isEqualTo(variantConfigAB.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfigAB.checkProjectDependencies)
        assertThat(config.remoteRepositoryUrl).isEqualTo(variantConfigAB.remoteRepositoryUrl)
        assertThat(config.mappingFilePath).isEqualTo(variantConfigAB.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(variantConfigAB.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(variantConfigAB.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(variantConfigAB.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w build type }`(
        @Forgery configuration: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName() +
            fakeBuildTypeName.replaceFirstChar { capitalizeChar(it) }
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(fakeExtension.variants.findByName(variantName)) doReturn configuration

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(configuration.versionName)
        assertThat(config.serviceName).isEqualTo(configuration.serviceName)
        assertThat(config.site).isEqualTo(configuration.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(configuration.checkProjectDependencies)
        assertThat(config.remoteRepositoryUrl).isEqualTo(configuration.remoteRepositoryUrl)
        assertThat(config.mappingFilePath).isEqualTo(configuration.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(configuration.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(configuration.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(configuration.ignoreDatadogCiFileConfig)
    }

    // endregion

    // region configureVariantForSdkCheck

    // TODO RUMM-2344 switch back to FAIL
    @Test
    fun `M use NONE when configuring checkDepsTask { checkProjectDependencies not set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String,
        @StringForgery configurationName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = null
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName

        fakeProject.task("compile${variantName.replaceFirstChar { capitalizeChar(it) }}Sources")

        val mockConfiguration = mock<Configuration>()
        whenever(mockConfiguration.name) doReturn configurationName

        whenever(mockVariant.compileConfiguration) doReturn mockConfiguration

        // When
        val checkSdkDepsTaskProvider = testedPlugin.configureVariantForSdkCheck(
            fakeProject,
            mockVariant,
            fakeExtension
        )

        // Then
        assertThat(checkSdkDepsTaskProvider).isNull()
    }

    @Test
    fun `M do nothing W configureVariantForSdkCheck() { none set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = SdkCheckLevel.NONE
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName

        fakeProject.task("compile${variantName.replaceFirstChar { capitalizeChar(it) }}Sources")

        // When + Then
        assertThat(
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )
        ).isNull()
    }

    @Test
    fun `M do nothing W configureVariantForSdkCheck() { extension is disabled }`() {
        // Given
        fakeExtension.enabled = false

        // When + Then
        assertThat(
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )
        ).isNull()
    }

    @Test
    fun `M do nothing W configureVariantForSdkCheck() { compilation task not found }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String
    ) {
        // let's check that there are no tasks just in case if setup is modified
        assertThat(fakeProject.tasks.isEmpty()).isTrue()

        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName

        assertThat(
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )
        ).isNull()
    }

    // endregion

    // region Internal

    private fun List<String>.variantName(): String {
        return first() + drop(1).joinToString("") { it.replaceFirstChar { capitalizeChar(it) } }
    }

    private fun mockBuildIdGenerationTask(buildId: String): TaskProvider<GenerateBuildIdTask> {
        return mock<TaskProvider<GenerateBuildIdTask>>().apply {
            whenever(
                flatMap(any<Transformer<Provider<String>, GenerateBuildIdTask>>())
            ) doReturn buildId.asProvider()
        }
    }

    private fun <T> T.asProvider(): Provider<T> {
        return fakeProject.provider { this }
    }

    private fun String.asFileProvider(): Provider<RegularFile> {
        return fakeProject.objects.fileProperty().value { File(this) }
    }

    // endregion
}
