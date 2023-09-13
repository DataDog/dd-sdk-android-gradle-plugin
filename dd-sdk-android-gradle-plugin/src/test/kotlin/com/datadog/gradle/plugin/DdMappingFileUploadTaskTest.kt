/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.DdAppIdentifier
import com.datadog.gradle.plugin.internal.Uploader
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class DdMappingFileUploadTaskTest {

    private lateinit var testedTask: DdMappingFileUploadTask

    @TempDir
    lateinit var tempDir: File

    @Mock
    lateinit var mockUploader: Uploader

    @Mock
    lateinit var mockRepositoryDetector: RepositoryDetector

    @StringForgery
    lateinit var fakeVariant: String

    @StringForgery
    lateinit var fakeVersion: String

    @StringForgery
    lateinit var fakeService: String

    lateinit var fakeApiKey: ApiKey

    @Forgery
    lateinit var fakeSite: DatadogSite

    @StringForgery(regex = "git@github\\.com:[a-z]+/[a-z][a-z0-9_-]+\\.git")
    lateinit var fakeRemoteUrl: String

    @StringForgery(regex = "[a-z]{8}\\.txt")
    lateinit var fakeMappingFileName: String

    @StringForgery(regex = "[a-z]{8}\\.txt")
    lateinit var fakeRepositoryFileName: String

    @StringForgery
    lateinit var fakeMappingFileContent: String

    @Forgery
    lateinit var fakeRepoInfo: RepositoryInfo

    @BeforeEach
    fun `set up`(forge: Forge) {
        val fakeProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        testedTask = fakeProject.tasks.create(
            "DdMappingFileUploadTask",
            DdMappingFileUploadTask::class.java,
            mockRepositoryDetector
        )

        testedTask.uploader = mockUploader
        fakeApiKey = ApiKey(
            value = forge.anHexadecimalString(),
            source = forge.aValueFrom(ApiKeySource::class.java)
        )
        testedTask.apiKey = fakeApiKey.value
        testedTask.apiKeySource = fakeApiKey.source
        testedTask.variantName = fakeVariant
        testedTask.versionName = fakeVersion
        testedTask.serviceName = fakeService
        testedTask.site = fakeSite.name
        setEnv(DdMappingFileUploadTask.DATADOG_SITE, "")
    }

    @AfterEach
    fun `tear down`() {
        removeEnv(DdMappingFileUploadTask.DATADOG_SITE)
    }

    @Test
    fun `𝕄 upload file 𝕎 applyTask()`() {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)
        testedTask.mappingFilePath = fakeMappingFile.path
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey.value,
            DdAppIdentifier(
                serviceName = fakeService,
                version = fakeVersion,
                variant = fakeVariant
            ),
            fakeRepoInfo,
            useGzip = true
        )
        assertThat(fakeRepositoryFile.readText())
            .isEqualTo(
                "{\"data\":[" + fakeRepoInfo.toJson().toString(0) + "],\"version\":1}"
            )
    }

    @Test
    fun `𝕄 upload file 𝕎 applyTask() { short aliases requested }`() {
        // Given
        testedTask.mappingFilePackagesAliases = mapOf(
            "androidx.fragment.app" to "axfraga",
            "androidx.activity" to "axact",
            "androidx.activity.ComponentActivity" to "axact.ca",
            "androidx.appcompat" to "axapp",
            "androidx.work" to "axw",
            "java.lang" to "jl",
            "kotlin.collections" to "kc"
        )
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fileFromResourcesPath("mapping.txt").copyTo(fakeMappingFile)

        testedTask.mappingFilePath = fakeMappingFile.path
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))

        // When
        testedTask.applyTask()

        // Then
        argumentCaptor<File> {
            verify(mockUploader).upload(
                eq(fakeSite),
                capture(),
                eq(fakeRepositoryFile),
                eq(fakeApiKey.value),
                eq(
                    DdAppIdentifier(
                        serviceName = fakeService,
                        version = fakeVersion,
                        variant = fakeVariant
                    )
                ),
                eq(fakeRepoInfo),
                useGzip = eq(true)
            )
            assertThat(lastValue).hasSameTextualContentAs(
                fileFromResourcesPath("mapping-with-aliases.txt")
            )
        }
    }

    @Test
    fun `𝕄 upload file 𝕎 applyTask() { trim starting indents }`(
        forge: Forge
    ) {
        // Given
        val expectedLines = forge.aList {
            forge.anAlphabeticalString() + forge.aString { ' ' }
        }
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(
            expectedLines.joinToString(separator = "\n") {
                val indent = if (forge.aBool()) forge.aString { ' ' } else ""
                indent + it
            }
        )

        testedTask.mappingFileTrimIndents = true
        testedTask.mappingFilePath = fakeMappingFile.path
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))

        // When
        testedTask.applyTask()

        // Then
        argumentCaptor<File> {
            verify(mockUploader).upload(
                eq(fakeSite),
                capture(),
                eq(fakeRepositoryFile),
                eq(fakeApiKey.value),
                eq(
                    DdAppIdentifier(
                        serviceName = fakeService,
                        version = fakeVersion,
                        variant = fakeVariant
                    )
                ),
                eq(fakeRepoInfo),
                useGzip = eq(true)
            )
            assertThat(lastValue.readLines()).isEqualTo(expectedLines)
        }
    }

    @Test
    fun `𝕄 upload file 𝕎 applyTask() { delete old shrinked mapping file before writing }`(
        forge: Forge
    ) {
        // Given
        val expectedLines = forge.aList {
            forge.anAlphabeticalString() + forge.aString { ' ' }
        }
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(
            expectedLines.joinToString(separator = "\n") {
                val indent = if (forge.aBool()) forge.aString { ' ' } else ""
                indent + it
            }
        )

        testedTask.mappingFileTrimIndents = true
        testedTask.mappingFilePath = fakeMappingFile.path
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))
        val oldShrinkedMappingFile = File(
            fakeMappingFile.parent,
            DdMappingFileUploadTask.MAPPING_OPTIMIZED_FILE_NAME
        )
        oldShrinkedMappingFile.createNewFile()
        oldShrinkedMappingFile.writeText(forge.aString())

        // When
        testedTask.applyTask()

        // Then
        argumentCaptor<File> {
            verify(mockUploader).upload(
                eq(fakeSite),
                capture(),
                eq(fakeRepositoryFile),
                eq(fakeApiKey.value),
                eq(
                    DdAppIdentifier(
                        serviceName = fakeService,
                        version = fakeVersion,
                        variant = fakeVariant
                    )
                ),
                eq(fakeRepoInfo),
                useGzip = eq(true)
            )
            assertThat(lastValue.readLines()).isEqualTo(expectedLines)
        }
    }

    @Test
    fun `𝕄 upload file 𝕎 applyTask { remote url provided }`() {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)
        testedTask.mappingFilePath = fakeMappingFile.path
        testedTask.remoteRepositoryUrl = fakeRemoteUrl
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), eq(fakeRemoteUrl)))
            .doReturn(listOf(fakeRepoInfo))

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey.value,
            DdAppIdentifier(
                serviceName = fakeService,
                version = fakeVersion,
                variant = fakeVariant
            ),
            fakeRepoInfo,
            useGzip = true
        )
        assertThat(fakeRepositoryFile.readText())
            .isEqualTo(
                "{\"data\":[" + fakeRepoInfo.toJson().toString(0) + "],\"version\":1}"
            )
    }

    @Test
    fun `𝕄 upload file 𝕎 applyTask() {not a git repo}`() {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)
        testedTask.mappingFilePath = fakeMappingFile.path
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(emptyList())

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            fakeMappingFile,
            null,
            fakeApiKey.value,
            DdAppIdentifier(
                serviceName = fakeService,
                version = fakeVersion,
                variant = fakeVariant
            ),
            null,
            useGzip = true
        )
    }

    @Test
    fun `𝕄 throw error 𝕎 applyTask() {no api key}`() {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)
        testedTask.mappingFilePath = fakeMappingFile.path
        testedTask.apiKey = ""
        testedTask.apiKeySource = ApiKeySource.NONE

        // When
        assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `𝕄 throw error 𝕎 applyTask() {api key contains quotes or apostrophes}`(
        forge: Forge
    ) {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)
        testedTask.mappingFilePath = fakeMappingFile.path
        testedTask.apiKey = forge.anAlphaNumericalString().let {
            val splitIndex = forge.anInt(min = 0, max = it.length) + 1
            it.substring(0, splitIndex) +
                forge.anElementFrom("\"", "'") +
                it.substring(splitIndex)
        }
        testedTask.apiKeySource = ApiKeySource.NONE

        // When
        assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `𝕄 throw error 𝕎 applyTask() {invalid site}`(
        @StringForgery siteName: String
    ) {
        assumeTrue(siteName !in listOf("US", "EU", "GOV"))

        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)
        testedTask.mappingFilePath = fakeMappingFile.path
        testedTask.site = siteName

        // When
        assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `𝕄 upload to US1 𝕎 applyTask() {missing site}`() {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)
        testedTask.mappingFilePath = fakeMappingFile.path
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        testedTask.site = ""
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            DatadogSite.US1,
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey.value,
            DdAppIdentifier(
                serviceName = fakeService,
                version = fakeVersion,
                variant = fakeVariant
            ),
            fakeRepoInfo,
            useGzip = true
        )
        assertThat(fakeRepositoryFile.readText())
            .isEqualTo(
                "{\"data\":[" + fakeRepoInfo.toJson().toString(0) + "],\"version\":1}"
            )
    }

    @Test
    fun `𝕄 do nothing 𝕎 applyTask() {no mapping file}`() {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        testedTask.mappingFilePath = fakeMappingFile.path

        // When
        testedTask.applyTask()

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `𝕄 throw error 𝕎 applyTask() {mapping file is dir}`() {
        // Given
        val fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.mkdirs()
        testedTask.mappingFilePath = fakeMappingFile.path

        // When
        assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `𝕄 apply datadog CI config if exists 𝕎 applyTask()`(forge: Forge) {
        // Given
        val fakeDatadogCiFile = File(tempDir, "datadog-ci.json")

        val fakeDatadogCiApiKey = forge.anAlphabeticalString()
        val fakeDatadogCiDomain = forge.aValueFrom(DatadogSite::class.java).domain

        fakeDatadogCiFile.writeText(
            JSONObject().apply {
                put("apiKey", fakeDatadogCiApiKey)
                put("datadogSite", fakeDatadogCiDomain)
            }.toString()
        )

        testedTask.apiKeySource = forge.aValueFrom(
            ApiKeySource::class.java,
            exclude = listOf(ApiKeySource.GRADLE_PROPERTY)
        )
        testedTask.datadogCiFile = fakeDatadogCiFile
        testedTask.site = ""

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeDatadogCiApiKey)
        assertThat(testedTask.apiKeySource).isEqualTo(ApiKeySource.DATADOG_CI_CONFIG_FILE)
        assertThat(testedTask.site).isEqualTo(DatadogSite.fromDomain(fakeDatadogCiDomain)?.name)
    }

    @Test
    fun `𝕄 apply datadog CI config if exists 𝕎 applyTask() {apiKey from gradle}`(forge: Forge) {
        // Given
        val fakeDatadogCiFile = File(tempDir, "datadog-ci.json")

        val fakeDatadogCiApiKey = forge.anAlphabeticalString()
        val fakeDatadogCiDomain = forge.aValueFrom(DatadogSite::class.java).domain

        fakeDatadogCiFile.writeText(
            JSONObject().apply {
                put("apiKey", fakeDatadogCiApiKey)
                put("datadogSite", fakeDatadogCiDomain)
            }.toString()
        )

        testedTask.apiKeySource = ApiKeySource.GRADLE_PROPERTY
        testedTask.datadogCiFile = fakeDatadogCiFile
        testedTask.site = ""

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.apiKeySource).isEqualTo(ApiKeySource.GRADLE_PROPERTY)
        assertThat(testedTask.site).isEqualTo(DatadogSite.fromDomain(fakeDatadogCiDomain)?.name)
    }

    @Test
    fun `𝕄 apply datadog CI config if exists 𝕎 applyTask() { apiKey is missing }`(forge: Forge) {
        // Given
        val fakeDatadogCiFile = File(tempDir, "datadog-ci.json")

        val fakeDatadogCiDomain = forge.aValueFrom(DatadogSite::class.java).domain

        fakeDatadogCiFile.writeText(
            JSONObject().apply {
                put("datadogSite", fakeDatadogCiDomain)
            }.toString()
        )

        testedTask.datadogCiFile = fakeDatadogCiFile
        testedTask.site = ""

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.site).isEqualTo(DatadogSite.fromDomain(fakeDatadogCiDomain)?.name)
    }

    @Test
    fun `𝕄 apply datadog CI config if exists 𝕎 applyTask() {datadogSite missing}`(forge: Forge) {
        // Given
        val fakeDatadogCiFile = File(tempDir, "datadog-ci.json")

        val fakeDatadogCiApiKey = forge.anAlphabeticalString()

        fakeDatadogCiFile.writeText(
            JSONObject().apply {
                put("apiKey", fakeDatadogCiApiKey)
            }.toString()
        )

        testedTask.apiKeySource = forge.aValueFrom(
            ApiKeySource::class.java,
            exclude = listOf(ApiKeySource.GRADLE_PROPERTY)
        )
        testedTask.datadogCiFile = fakeDatadogCiFile

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeDatadogCiApiKey)
        assertThat(testedTask.apiKeySource).isEqualTo(ApiKeySource.DATADOG_CI_CONFIG_FILE)
        assertThat(testedTask.site).isEqualTo(fakeSite.name)
    }

    @Test
    fun `𝕄 apply datadog CI config if exists 𝕎 applyTask() {datadogSite unknown}`(forge: Forge) {
        // Given
        val fakeDatadogCiFile = File(tempDir, "datadog-ci.json")

        val fakeDatadogCiDomain = forge.aStringMatching("[a-z]+\\.com")

        fakeDatadogCiFile.writeText(
            JSONObject().apply {
                put("datadogSite", fakeDatadogCiDomain)
            }.toString()
        )

        testedTask.datadogCiFile = fakeDatadogCiFile
        testedTask.site = ""

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(testedTask.site).isEqualTo(DatadogSite.US1.name)
    }

    @Test
    fun `𝕄 apply datadog CI config if exists 𝕎 applyTask() {site is set already}`(forge: Forge) {
        // Given
        val fakeDatadogCiFile = File(tempDir, "datadog-ci.json")

        val fakeDatadogCiDomain = forge.aValueFrom(DatadogSite::class.java).domain

        fakeDatadogCiFile.writeText(
            JSONObject().apply {
                put("datadogSite", fakeDatadogCiDomain)
            }.toString()
        )

        testedTask.datadogCiFile = fakeDatadogCiFile

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(testedTask.site).isEqualTo(fakeSite.name)
    }

    @Test
    fun `𝕄 read site from environment variable 𝕎 applyTask() {site is not set}`(forge: Forge) {
        // Given
        val fakeDatadogEnvDomain = forge.aValueFrom(DatadogSite::class.java).domain
        setEnv(DdMappingFileUploadTask.DATADOG_SITE, fakeDatadogEnvDomain)
        testedTask.site = ""

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(testedTask.site).isEqualTo(DatadogSite.fromDomain(fakeDatadogEnvDomain)?.name)
    }

    @Test
    fun `𝕄 read site from environment variable 𝕎 applyTask() {site is set}`(forge: Forge) {
        // Given
        val fakeDatadogEnvDomain = forge.aValueFrom(DatadogSite::class.java).domain
        setEnv(DdAndroidGradlePlugin.DATADOG_API_KEY, fakeDatadogEnvDomain)

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(testedTask.site).isEqualTo(fakeSite.name)
    }

    @Test
    fun `𝕄 not apply datadog CI config if exists 𝕎 applyTask() { malformed json }`(forge: Forge) {
        // Given
        val fakeDatadogCiFile = File(tempDir, "datadog-ci.json")

        fakeDatadogCiFile.writeText(forge.anElementFrom(forge.aString(), JSONArray().toString()))

        testedTask.datadogCiFile = fakeDatadogCiFile

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(testedTask.site).isEqualTo(fakeSite.name)
    }

    // region private

    private fun fileFromResourcesPath(resourceFilePath: String): File {
        return File(javaClass.classLoader.getResource(resourceFilePath)!!.file)
    }

    // endregion
}
