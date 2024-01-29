package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.Uploader
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
internal class DdSymbolFileUploadTaskTest {
    private lateinit var testedTask: DdSymbolFileUploadTask

    @TempDir
    lateinit var tempDir: File

    @Mock
    lateinit var mockUploader: Uploader

    @Mock
    lateinit var mockRepositoryDetector: RepositoryDetector

    lateinit var fakeBuildId: String

    lateinit var fakeApiKey: ApiKey

    @Forgery
    lateinit var fakeSite: DatadogSite

    @BeforeEach
    fun `set up`(forge: Forge) {
        val fakeProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        testedTask = fakeProject.tasks.create(
            "DdSymbolFileUploadTask",
            DdSymbolFileUploadTask::class.java,
            mockRepositoryDetector
        )
        testedTask.uploader = mockUploader
        fakeApiKey = ApiKey(
            value = forge.anHexadecimalString(),
            source = forge.aValueFrom(ApiKeySource::class.java)
        )
        fakeBuildId = forge.getForgery<UUID>().toString()

        testedTask.apiKey = fakeApiKey.value
        testedTask.apiKeySource = fakeApiKey.source
        testedTask.buildId = mock<Provider<String>>().apply {
            whenever(isPresent) doReturn true
            whenever(get()) doReturn fakeBuildId
        }
        setEnv(DdMappingFileUploadTask.DATADOG_SITE, "")
    }

    @AfterEach
    fun `tear down`() {
        removeEnv(DdMappingFileUploadTask.DATADOG_SITE)
    }

    @Test
    fun `M upload file W applyTask()`() {

    }
}