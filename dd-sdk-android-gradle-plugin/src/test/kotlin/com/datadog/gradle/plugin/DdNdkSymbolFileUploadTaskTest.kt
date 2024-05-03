package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.DdAppIdentifier
import com.datadog.gradle.plugin.internal.Uploader
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.provider.Provider
import org.gradle.internal.impldep.org.junit.Assume.assumeTrue
import org.gradle.testfixtures.ProjectBuilder
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
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
import org.mockito.kotlin.mock
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
internal class DdNdkSymbolFileUploadTaskTest {
    private lateinit var testedTask: DdNdkSymbolFileUploadTask

    @TempDir
    lateinit var tempDir: File

    @Mock
    lateinit var mockUploader: Uploader

    @Mock
    lateinit var mockVariant: ApplicationVariant

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
        whenever(mockVariant.versionName).thenReturn(fakeVersion)
        whenever(mockVariant.versionCode).thenReturn(fakeVersionCode)

        testedTask = fakeProject.tasks.create(
            "DdSymbolFileUploadTask",
            DdNdkSymbolFileUploadTask::class.java,
            mockRepositoryDetector
        )
        testedTask.uploader = mockUploader
        fakeApiKey = ApiKey(
            value = forge.anHexadecimalString(),
            source = forge.aValueFrom(ApiKeySource::class.java)
        )
        fakeBuildId = forge.getForgery<UUID>().toString()

        testedTask.searchDirectories.from(tempDir)
        testedTask.buildId = mock<Provider<String>>().apply {
            whenever(isPresent) doReturn true
            whenever(get()) doReturn fakeBuildId
        }

        val fakeConfiguration = with(DdExtensionConfiguration()) {
            versionName = fakeVersion
            serviceName = fakeService
            site = fakeSite.toString()
            this
        }
        testedTask.configureWith(
            fakeApiKey,
            fakeConfiguration,
            mockVariant
        )
        setEnv(DdFileUploadTask.DATADOG_SITE, "")
    }

    @AfterEach
    fun `tear down`() {
        removeEnv(DdFileUploadTask.DATADOG_SITE)
    }

    @Test
    fun `M upload file W applyTask()`() {
        // Given
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))
        val fakeSoFile = writeFakeSoFile("arm64-v8a")

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            Uploader.UploadFileInfo(
                fileKey = DdNdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                file = fakeSoFile,
                encoding = DdNdkSymbolFileUploadTask.ENCODING,
                fileType = DdNdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
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
        testedTask.repositoryFile = fakeRepositoryFile
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
                    fileKey = DdNdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                    file = it.value,
                    encoding = DdNdkSymbolFileUploadTask.ENCODING,
                    fileType = DdNdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
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
        testedTask.repositoryFile = fakeRepositoryFile
        testedTask.remoteRepositoryUrl = fakeRemoteUrl
        whenever(mockRepositoryDetector.detectRepositories(any(), eq(fakeRemoteUrl)))
            .doReturn(listOf(fakeRepoInfo))
        val fakeSoFile = writeFakeSoFile("arm64-v8a")

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            Uploader.UploadFileInfo(
                fileKey = DdNdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                file = fakeSoFile,
                encoding = DdNdkSymbolFileUploadTask.ENCODING,
                fileType = DdNdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
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
        testedTask.repositoryFile = fakeRepositoryFile
        whenever(mockRepositoryDetector.detectRepositories(any(), any()))
            .doReturn(emptyList())
        val fakeSoFile = writeFakeSoFile("arm64-v8a")

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            fakeSite,
            Uploader.UploadFileInfo(
                fileKey = DdNdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                file = fakeSoFile,
                encoding = DdNdkSymbolFileUploadTask.ENCODING,
                fileType = DdNdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
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
        testedTask.apiKey = ""
        testedTask.apiKeySource = ApiKeySource.NONE
        writeFakeSoFile("arm64-v8a")

        // When
        val error = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(error.message).isEqualTo(DdFileUploadTask.API_KEY_MISSING_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {api key contains quotes or apostrophes}`(
        forge: Forge
    ) {
        // Given
        testedTask.apiKey = forge.anAlphaNumericalString().let {
            val splitIndex = forge.anInt(min = 0, max = it.length) + 1
            it.substring(0, splitIndex) +
                forge.anElementFrom("\"", "'") +
                it.substring(splitIndex)
        }
        writeFakeSoFile("arm64")

        // When
        val exception = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(exception.message)
            .isEqualTo(DdFileUploadTask.INVALID_API_KEY_FORMAT_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {buildId is missing}`() {
        // Given
        whenever(testedTask.buildId.isPresent) doReturn false
        writeFakeSoFile("arm64-v8a")

        // When
        val exception = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(exception.message).isEqualTo(DdFileUploadTask.MISSING_BUILD_ID_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {buildId is empty string}`() {
        // Given
        whenever(testedTask.buildId.isPresent) doReturn true
        whenever(testedTask.buildId.get()) doReturn ""
        writeFakeSoFile("arm64-v8a")

        // When
        val exception = assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        assertThat(exception.message).isEqualTo(DdFileUploadTask.MISSING_BUILD_ID_ERROR)
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M throw error W applyTask() {invalid site}`(
        @StringForgery siteName: String
    ) {
        assumeTrue(siteName !in listOf("US", "EU", "GOV"))

        // Given
        testedTask.site = siteName

        // When
        assertThrows<IllegalStateException> {
            testedTask.applyTask()
        }

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M upload to US1 W applyTask() {missing site}`() {
        // Given
        val fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        testedTask.repositoryFile = fakeRepositoryFile
        testedTask.site = ""
        whenever(mockRepositoryDetector.detectRepositories(any(), eq("")))
            .doReturn(listOf(fakeRepoInfo))
        val fakeSoFile = writeFakeSoFile("arm64-v8a")

        // When
        testedTask.applyTask()

        // Then
        verify(mockUploader).upload(
            DatadogSite.US1,
            Uploader.UploadFileInfo(
                fileKey = DdNdkSymbolFileUploadTask.KEY_NDK_SYMBOL_FILE,
                file = fakeSoFile,
                encoding = DdNdkSymbolFileUploadTask.ENCODING,
                fileType = DdNdkSymbolFileUploadTask.TYPE_NDK_SYMBOL_FILE,
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
    fun `M do nothing W applyTask() {no mapping file}`() {
        // Given

        // When
        testedTask.applyTask()

        // Then
        verifyNoInteractions(mockUploader)
    }

    @Test
    fun `M apply datadog CI config if exists W applyTask()`(forge: Forge) {
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
    fun `M apply datadog CI config if exists W applyTask() {apiKey from gradle}`(forge: Forge) {
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
    fun `M apply datadog CI config if exists W applyTask() { apiKey is missing }`(forge: Forge) {
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
    fun `M apply datadog CI config if exists W applyTask() {datadogSite missing}`(forge: Forge) {
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
    fun `M apply datadog CI config if exists W applyTask() {datadogSite unknown}`(forge: Forge) {
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
    fun `M apply datadog CI config if exists W applyTask() {site is set already}`(forge: Forge) {
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
    fun `M read site from environment variable W applyTask() {site is not set}`(forge: Forge) {
        // Given
        val fakeDatadogEnvDomain = forge.aValueFrom(DatadogSite::class.java).domain
        setEnv(DdFileUploadTask.DATADOG_SITE, fakeDatadogEnvDomain)
        testedTask.site = ""

        // When
        testedTask.applyTask()

        // Then
        assertThat(testedTask.apiKey).isEqualTo(fakeApiKey.value)
        assertThat(testedTask.apiKeySource).isEqualTo(fakeApiKey.source)
        assertThat(testedTask.site).isEqualTo(DatadogSite.fromDomain(fakeDatadogEnvDomain)?.name)
    }

    @Test
    fun `M read site from environment variable W applyTask() {site is set}`(forge: Forge) {
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
    fun `M not apply datadog CI config if exists W applyTask() { malformed json }`(forge: Forge) {
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

    private fun writeFakeSoFile(arch: String): File {
        val soTempDir = File(tempDir, "fakeSos/$arch")
        val fakeSoFile = File(soTempDir, "libfake.so")

        fakeSoFile.parentFile.mkdirs()
        fakeSoFile.writeText("fake")

        return fakeSoFile
    }
}
