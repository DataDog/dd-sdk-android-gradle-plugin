/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.utils.capitalizeChar
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.AdvancedForgery
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.MapForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

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
    lateinit var mockVariant: ApplicationVariant

    @Mock
    lateinit var mockBuildType: BuildType

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
        fakeProject = ProjectBuilder.builder().build()
        testedPlugin = DdAndroidGradlePlugin(mock())
        setEnv(DdAndroidGradlePlugin.DD_API_KEY, "")
        setEnv(DdAndroidGradlePlugin.DATADOG_API_KEY, "")
    }

    // region configureVariant()

    @Test
    fun `𝕄 configure the upload task with the variant info 𝕎 configureVariant()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.versionName = null
        fakeExtension.serviceName = null
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn
            "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(task.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(task.mappingFilePackagesAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases)
        assertThat(task.datadogCiFile).isNull()
    }

    @Test
    fun `𝕄 configure the upload task with the extension info 𝕎 configureVariant()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(task.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(task.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(task.mappingFilePackagesAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases)
        assertThat(task.mappingFileTrimIndents)
            .isEqualTo(fakeExtension.mappingFileTrimIndents)
        assertThat(task.datadogCiFile).isNull()
    }

    @Test
    fun `𝕄 configure the upload task with sanitized mapping aliases 𝕎 configureVariant()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String,
        forge: Forge
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

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
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(task.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(task.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(task.mappingFilePackagesAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases.minus(aliasToFilterOut.first))
        assertThat(task.mappingFileTrimIndents)
            .isEqualTo(fakeExtension.mappingFileTrimIndents)
        assertThat(task.datadogCiFile).isNull()
    }

    @Test
    fun `𝕄 use sensible defaults 𝕎 configureVariant() { empty config }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
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
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.remoteRepositoryUrl).isEmpty()
        assertThat(task.site).isEqualTo("")
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
        assertThat(task.mappingFilePackagesAliases).isEmpty()
        assertThat(task.mappingFileTrimIndents).isFalse
        assertThat(task.datadogCiFile).isNull()
    }

    @Test
    fun `𝕄 apply datadog CI file 𝕎 configureVariant()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
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
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        val fakeDatadogCiFile = File(fakeProject.projectDir, "datadog-ci.json")
        fakeDatadogCiFile.createNewFile()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.remoteRepositoryUrl).isEmpty()
        assertThat(task.site).isEqualTo("")
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
        assertThat(task.mappingFilePackagesAliases).isEmpty()
        assertThat(task.mappingFileTrimIndents).isFalse
        assertThat(task.datadogCiFile).isEqualTo(fakeDatadogCiFile)
    }

    @Test
    fun `𝕄 not apply datadog CI file 𝕎 configureVariant() { ignoreDatadogCiFileConfig }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
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
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        val fakeDatadogCiFile = File(fakeProject.projectDir, "datadog-ci.json")
        fakeDatadogCiFile.createNewFile()

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo(
            "uploadMapping${variantName.replaceFirstChar { capitalizeChar(it) }}"
        )
        assertThat(task.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(task.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.remoteRepositoryUrl).isEmpty()
        assertThat(task.site).isEqualTo("")
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
        assertThat(task.mappingFilePackagesAliases).isEmpty()
        assertThat(task.mappingFileTrimIndents).isFalse
        assertThat(task.datadogCiFile).isNull()
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariant() {minify disabled}`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn false
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        assertThat(task).isNull()
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariant() {plugin disabled}`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.enabled = false
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        assertThat(task).isNull()
    }

    // endregion

    // region resolveApiKey

    @Test
    fun `𝕄 resolve API KEY from project properties 𝕎 resolveApiKey()`() {
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
    fun `𝕄 resolve API KEY from environment variable 𝕎 resolveApiKey()`() {
        // Given
        setEnv(DdAndroidGradlePlugin.DD_API_KEY, fakeApiKey.value)

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey.value).isEqualTo(fakeApiKey.value)
        assertThat(apiKey.source).isEqualTo(ApiKeySource.ENVIRONMENT)
    }

    @Test
    fun `𝕄 resolve API KEY from alternative environment variable 𝕎 resolveApiKey()`() {
        // Given
        setEnv(DdAndroidGradlePlugin.DATADOG_API_KEY, fakeApiKey.value)

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey.value).isEqualTo(fakeApiKey.value)
        assertThat(apiKey.source).isEqualTo(ApiKeySource.ENVIRONMENT)
    }

    @Test
    fun `𝕄 returns empty String 𝕎 resolveApiKey() {key not defined anywhere}`() {
        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey).isEqualTo(ApiKey.NONE)
    }

    // endregion

    // region resolveExtensionConfiguration

    @Test
    fun `𝕄 return default config 𝕎 resolveExtensionConfiguration() {no variant config}`() {
        // When
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return config 𝕎 resolveExtensionConfiguration() { variant w full config }`(
        @Forgery variantConfig: DdExtensionConfiguration
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w version only }`(
        @StringForgery versionName: String
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w service only }`(
        @StringForgery serviceName: String
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w mappingPath }`(
        @StringForgery(regex = "/([a-z]+)/([a-z]+)/([a-z]+)/mapping.txt") mappingFilePath: String
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return config 𝕎 resolveExtensionConfiguration() { variant+mappingFilePackageAliases }`(
        @MapForgery(
            key = AdvancedForgery(
                string = [StringForgery(regex = "[a-z]{3}(\\.[a-z]{5,10}){2,4}")]
            ),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ALPHABETICAL)])
        ) mappingFilePackageAliases: Map<String, String>
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return config 𝕎 resolveExtensionConfiguration() { variant+mappingFileTrimIndents }`(
        @BoolForgery mappingFileTrimIndents: Boolean
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w site only }`(
        @Forgery site: DatadogSite
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w sdkCheck only }`(
        @Forgery sdkCheckLevel: SdkCheckLevel
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w remoteUrl only }`(
        @Forgery fakeConfig: DdExtensionConfiguration
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant + ignoreDdConfig }`(
        @Forgery fakeConfig: DdExtensionConfiguration
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { simple variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigA: DdExtensionConfiguration,
        @Forgery variantConfigB: DdExtensionConfiguration,
        @Forgery variantConfigC: DdExtensionConfiguration
    ) {
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
        mockVariant.mockFlavors(flavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { complex variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigAB: DdExtensionConfiguration,
        @Forgery variantConfigAC: DdExtensionConfiguration,
        @Forgery variantConfigBC: DdExtensionConfiguration
    ) {
        val flavorNames = listOf(flavorA, flavorB, flavorC)
        variantConfigAB.apply { versionName = null }
        variantConfigAC.apply { serviceName = null }
        variantConfigBC.apply { site = null }
        variantConfigBC.apply { checkProjectDependencies = null }
        mockVariant.mockFlavors(flavorNames, fakeBuildTypeName)
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
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w build type }`(
        @Forgery configuration: DdExtensionConfiguration
    ) {
        val variantName = fakeFlavorNames.variantName() +
            fakeBuildTypeName.replaceFirstChar { capitalizeChar(it) }
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
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
    fun `𝕄 use NONE when configuring checkDepsTask { checkProjectDependencies not set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String,
        @StringForgery configurationName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = null
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        fakeProject.task("compile${variantName.replaceFirstChar { capitalizeChar(it) }}Sources")

        val mockConfiguration = mock<Configuration>()
        whenever(mockConfiguration.name) doReturn configurationName

        whenever(mockVariant.compileConfiguration) doReturn mockConfiguration

        // When + Then
        val checkSdkDepsTaskProvider = testedPlugin.configureVariantForSdkCheck(
            fakeProject,
            mockVariant,
            fakeExtension
        )

        assertThat(checkSdkDepsTaskProvider).isNull()
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariantForSdkCheck() { none set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = SdkCheckLevel.NONE
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

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
    fun `𝕄 do nothing 𝕎 configureVariantForSdkCheck() { extension is disabled }`() {
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
    fun `𝕄 do nothing 𝕎 configureVariantForSdkCheck() { compilation task not found }`(
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

    // region findDatadogCiFile

    @Test
    fun `𝕄 find datadog-ci file 𝕎 findDatadogCiFile()`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, maxDepth = 3, forge = forge)

        File(tree[forge.anInt(0, tree.size)], "datadog-ci.json").createNewFile()

        // When
        val ciFile = testedPlugin.findDatadogCiFile(tree.last())

        // Then
        assertThat(ciFile).isNotNull()
    }

    @Test
    fun `𝕄 return null 𝕎 findDatadogCiFile() { no ci file found }`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, maxDepth = 3, forge = forge)

        // When
        val ciFile = testedPlugin.findDatadogCiFile(tree.last())

        // Then
        assertThat(ciFile).isNull()
    }

    @Test
    fun `𝕄 return null 𝕎 findDatadogCiFile() { beyond max levels up }`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, minDepth = 4, maxDepth = 7, forge = forge)

        // When
        val ciFile = testedPlugin.findDatadogCiFile(tree.last())

        // Then
        assertThat(ciFile).isNull()
    }

    // endregion

    // region Internal

    private fun List<String>.variantName(): String {
        return first() + drop(1).joinToString("") { it.replaceFirstChar { capitalizeChar(it) } }
    }

    private fun ApplicationVariant.mockFlavors(
        flavorNames: List<String>,
        buildTypeName: String
    ) {
        val mockFlavors: MutableList<ProductFlavor> = mutableListOf()
        for (flavorName in flavorNames) {
            mockFlavors.add(
                mock<ProductFlavor>().apply {
                    whenever(this.name) doReturn flavorName
                }
            )
        }
        val mockBuildType: BuildType = mock()
        whenever(mockBuildType.name) doReturn buildTypeName

        whenever(productFlavors) doReturn mockFlavors
        whenever(buildType) doReturn mockBuildType
    }

    private fun buildDirectoryTree(
        rootDir: File,
        minDepth: Int = 1,
        maxDepth: Int,
        forge: Forge
    ): List<File> {
        var currentDir = rootDir
        val tree = mutableListOf(rootDir)
        val stopAt = forge.anInt(min = minDepth, max = maxDepth + 1)
        for (level in 1..maxDepth) {
            if (level == stopAt) break

            currentDir = File(currentDir, forge.anAlphabeticalString())
            tree.add(currentDir)
        }

        tree.last().mkdirs()

        return tree
    }

    // endregion
}
