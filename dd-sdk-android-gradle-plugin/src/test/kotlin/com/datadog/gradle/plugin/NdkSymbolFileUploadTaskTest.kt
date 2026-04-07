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
import com.datadog.gradle.plugin.internal.variant.AppVariant
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
internal class NdkSymbolFileUploadTaskTest {

    private lateinit var testedTask: NdkSymbolFileUploadTask

    @TempDir
    lateinit var tempDir: File

    @Mock
    lateinit var mockUploader: Uploader

    @Mock
    lateinit var mockVariant: AppVariant

    @Mock
    lateinit var mockRepositoryDetector: RepositoryDetector

    @StringForgery
    lateinit var fakeVariantName: String

    lateinit var fakeBuildId: String

    @StringForgery
    lateinit var fakeVersion: String

    @IntForgery(min = 0)
    var fakeVersionCode: Int = 0

    @StringForgery
    lateinit var fakeService: String

    lateinit var fakeApiKey: ApiKey

    @StringForgery(regex = "[a-z]{8}\\.txt")
    lateinit var fakeRepositoryFileName: String

    @Forgery
    lateinit var fakeRepoInfo: RepositoryInfo

    @Forgery
    lateinit var fakeSite: DatadogSite

    @BeforeEach
    fun `set up`(forge: Forge) {
        val fakeProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()
        whenever(mockVariant.flavorName).thenReturn(fakeVariantName)
        whenever(mockVariant.versionName).thenReturn(fakeProject.provider { fakeVersion })
        whenever(mockVariant.versionCode).thenReturn(fakeProject.provider { fakeVersionCode })

        testedTask = fakeProject.tasks.register(
            "SymbolFileUploadTask",
            NdkSymbolFileUploadTask::class.java,
            mockRepositoryDetector
        ).get()
        testedTask.uploader = mockUploader
        testedTask.site.set(fakeSite.name)
        fakeApiKey = ApiKey(
            value = forge.anHexadecimalString(),
            source = forge.aValueFrom(ApiKeySource::class.java)
        )
        fakeBuildId = forge.getForgery<UUID>().toString()

        testedTask.searchDirectories.from(tempDir)
        writeBuildIdFile(fakeBuildId)

        val fakeConfiguration = with(DdExtensionConfiguration()) {
            versionName = fakeVersion
            serviceName = fakeService
            site = fakeSite.toString()
            this
        }
        testedTask.configureWith(
            fakeProject.providers.provider { fakeApiKey },
            fakeConfiguration,
            mockVariant
        )
    }

    @Test
    fun `M upload file W applyTask()`() {
        // Given
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile.set(fakeRepositoryFile)
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))
        val fakeSoFile = writeFakeSoFile("arm64-v8a")

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            Uploader.UploadFileInfo(
                fileKey = NdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                file = fakeSoFile,
                encoding = NdkSymbolFileUploadTask.ENCODING,
                fileType = NdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
                fileName = "libfake.so",
                extraAttributes = mapOf(
                    "arch" to "arm64"
                )
            ),
            fakeRepositoryFile,
            fakeApiKey.value,
            DdAppIdentifier(
                serviceName = fakeService,
                version = fakeVersion,
                versionCode = fakeVersionCode,
                variant = fakeVariantName,
                buildId = fakeBuildId
            ),
            fakeRepoInfo,
            useGzip = true,
            emulateNetworkCall = false
        )
    }

    @Test
    fun `M upload multiple files W applyTask()`() {
        // Given
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile.set(fakeRepositoryFile)
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))
        val fakeSoFiles = mapOf(
            "arm64" to writeFakeSoFile("arm64-v8a"),
            "x86" to writeFakeSoFile("x86")
        )

        // When
        testedTask.applyTask()

        // Then
        fakeSoFiles.forEach {
            verify(mockUploader).upload(
                fakeSite,
                Uploader.UploadFileInfo(
                    fileKey = NdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                    file = it.value,
                    encoding = NdkSymbolFileUploadTask.ENCODING,
                    fileType = NdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
                    fileName = "libfake.so",
                    extraAttributes = mapOf(
                        "arch" to it.key
                    )
                ),
                fakeRepositoryFile,
                fakeApiKey.value,
                DdAppIdentifier(
                    serviceName = fakeService,
                    version = fakeVersion,
                    versionCode = fakeVersionCode,
                    variant = fakeVariantName,
                    buildId = fakeBuildId
                ),
                fakeRepoInfo,
                useGzip = true,
                emulateNetworkCall = false
            )
        }
    }

    @Test
    fun `M upload multiple files W applyTask() { ignore unsupported arch }`(
        @StringForgery fakeArch: String
    ) {
        // Given
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile.set(fakeRepositoryFile)
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))

        val fakeSoFiles = mapOf(
            "arm64" to writeFakeSoFile("arm64-v8a"),
            "x86" to writeFakeSoFile("x86"),
            fakeArch to writeFakeSoFile(fakeArch)
        )

        // When
        testedTask.applyTask()

        // Then
        fakeSoFiles.minus(fakeArch).forEach {
            verify(mockUploader).upload(
                fakeSite,
                Uploader.UploadFileInfo(
                    fileKey = NdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                    file = it.value,
                    encoding = NdkSymbolFileUploadTask.ENCODING,
                    fileType = NdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
                    fileName = "libfake.so",
                    extraAttributes = mapOf(
                        "arch" to it.key
                    )
                ),
                fakeRepositoryFile,
                fakeApiKey.value,
                DdAppIdentifier(
                    serviceName = fakeService,
                    version = fakeVersion,
                    versionCode = fakeVersionCode,
                    variant = fakeVariantName,
                    buildId = fakeBuildId
                ),
                fakeRepoInfo,
                useGzip = true,
                emulateNetworkCall = false
            )
        }
    }

    @Test
    fun `M upload file W applyTask { remote url provided }`(
        @StringForgery(regex = "git@github\\.com:[a-z]+/[a-z][a-z0-9_-]+\\.git")
        fakeRemoteUrl: String
    ) {
        // Given
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile.set(fakeRepositoryFile)
        testedTask.remoteRepositoryUrl.set(fakeRemoteUrl)
        whenever(mockRepositoryDetector.detectRepositories(any(), eq(fakeRemoteUrl)))
            .doReturn(listOf(fakeRepoInfo))
        val fakeSoFile = writeFakeSoFile("arm64-v8a")

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            Uploader.UploadFileInfo(
                fileKey = NdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                file = fakeSoFile,
                encoding = NdkSymbolFileUploadTask.ENCODING,
                fileType = NdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
                fileName = "libfake.so",
                extraAttributes = mapOf(
                    "arch" to "arm64"
                )
            ),
            fakeRepositoryFile,
            fakeApiKey.value,
            DdAppIdentifier(
                serviceName = fakeService,
                version = fakeVersion,
                versionCode = fakeVersionCode,
                variant = fakeVariantName,
                buildId = fakeBuildId
            ),
            fakeRepoInfo,
            useGzip = true,
            emulateNetworkCall = false
        )
        Assertions.assertThat(fakeRepositoryFile.readText())
            .isEqualTo(
                "{\"data\":[" + fakeRepoInfo.toJson().toString(0) + "],\"version\":1}"
            )
    }

    @Test
    fun `M upload file W applyTask { not a git repo }`() {
        // Given
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile.set(fakeRepositoryFile)
        whenever(mockRepositoryDetector.detectRepositories(any(), any()))
            .doReturn(emptyList())
        val fakeSoFile = writeFakeSoFile("arm64-v8a")

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            Uploader.UploadFileInfo(
                fileKey = NdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                file = fakeSoFile,
                encoding = NdkSymbolFileUploadTask.ENCODING,
                fileType = NdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
                fileName = "libfake.so",
                extraAttributes = mapOf(
                    "arch" to "arm64"
                )
            ),
            null,
            fakeApiKey.value,
            DdAppIdentifier(
                serviceName = fakeService,
                version = fakeVersion,
                versionCode = fakeVersionCode,
                variant = fakeVariantName,
                buildId = fakeBuildId
            ),
            null,
            useGzip = true,
            emulateNetworkCall = false
        )
    }

    @Test
    fun `M throw error when applyTask() { no api key }`() {
        // Given
        testedTask.apiKey.set("")
        testedTask.apiKeySource.set(ApiKeySource.NONE)
        writeFakeSoFile("arm64-v8a")

        // When
        val error = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(error.message).isEqualTo(FileUploadTask.API_KEY_MISSING_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {api key contains quotes or apostrophes}`(
        forge: Forge
    ) {
        // Given
        testedTask.apiKey.set(
            forge.anAlphaNumericalString().let {
                val splitIndex = forge.anInt(min = 0, max = it.length) + 1
                it.substring(0, splitIndex) +
                    forge.anElementFrom("\"", "'") +
                    it.substring(splitIndex)
            }
        )
        writeFakeSoFile("arm64")

        // When
        val exception = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(exception.message)
            .isEqualTo(FileUploadTask.INVALID_API_KEY_FORMAT_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {buildId is missing}`() {
        // Given
        testedTask.buildIdFile.set(null as File?)
        writeFakeSoFile("arm64-v8a")

        // When
        val exception = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(exception.message).isEqualTo(FileUploadTask.MISSING_BUILD_ID_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {buildId is empty string}`() {
        // Given
        writeBuildIdFile("")
        writeFakeSoFile("arm64-v8a")

        // When
        val exception = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(exception.message).isEqualTo(FileUploadTask.MISSING_BUILD_ID_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {invalid site}`(
        @StringForgery siteName: String
    ) {
        assumeTrue(siteName !in listOf("US", "EU", "GOV"))

        // Given
        testedTask.site.set(siteName)

        // When
        assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M do nothing W applyTask() {no mapping file}`() {
        // Given

        // When
        testedTask.applyTask()

        // Then
        verifyNoInteractions(mockUploader)
    }

    private fun writeFakeSoFile(arch: String): File {
        val soTempDir = File(tempDir, "fakeSos/$arch")
        val fakeSoFile = File(soTempDir, "libfake.so")

        fakeSoFile.parentFile.mkdirs()
        fakeSoFile.writeText("fake")

        return fakeSoFile
    }

    private fun writeBuildIdFile(buildId: String) {
        val buildIdFile = File(tempDir, GenerateBuildIdTask.BUILD_ID_FILE_NAME)
        buildIdFile.writeText(buildId)
        testedTask.buildIdFile.fileValue(buildIdFile)
    }
}
