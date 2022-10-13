/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.Configurator
import com.datadog.gradle.plugin.RecordedRequestAssert.Companion.assertThat
import com.datadog.gradle.plugin.RepositoryInfo
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.io.File
import java.lang.IllegalStateException
import java.net.HttpURLConnection

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class OkHttpUploaderTest {

    lateinit var testedUploader: OkHttpUploader

    @TempDir
    lateinit var tempDir: File

    lateinit var fakeMappingFile: File
    lateinit var fakeRepositoryFile: File

    @Forgery
    lateinit var fakeIdentifier: DdAppIdentifier

    @StringForgery(StringForgeryType.HEXADECIMAL)
    lateinit var fakeApiKey: String

    @StringForgery(regex = "[a-z]{8}\\.txt")
    lateinit var fakeMappingFileName: String

    @StringForgery(regex = "[a-z]{8}\\.txt")
    lateinit var fakeRepositoryFileName: String

    @Forgery
    lateinit var fakeRepositoryInfo: RepositoryInfo

    @StringForgery
    lateinit var fakeMappingFileContent: String

    @StringForgery
    lateinit var fakeRepositoryFileContent: String

    lateinit var mockWebServer: MockWebServer

    lateinit var mockResponse: MockResponse

    lateinit var mockDispatcher: Dispatcher

    var dispatchedRequest: RecordedRequest? = null

    @BeforeEach
    fun `set up`() {
        fakeMappingFile = File(tempDir, fakeMappingFileName)
        fakeMappingFile.writeText(fakeMappingFileContent)

        fakeRepositoryFile = File(tempDir, fakeRepositoryFileName)
        fakeRepositoryFile.writeText(fakeRepositoryFileContent)

        mockWebServer = MockWebServer()
        mockDispatcher = MockDispatcher()
        mockWebServer.dispatcher = mockDispatcher
        testedUploader = OkHttpUploader()
    }

    @AfterEach
    fun `tear down`() {
        mockWebServer.shutdown()
        dispatchedRequest = null
    }

    @Test
    fun `M set unlimited client callTimeout W init()`() {
        assertThat(testedUploader.client.callTimeoutMillis).isEqualTo(0)
    }

    @Test
    fun `M set client writeTimeout W init()`() {
        assertThat(testedUploader.client.writeTimeoutMillis).isEqualTo(
            OkHttpUploader.NETWORK_TIMEOUT_MS.toInt()
        )
    }

    @Test
    fun `M set client connectTimeout W init()`() {
        assertThat(testedUploader.client.connectTimeoutMillis).isEqualTo(
            OkHttpUploader.NETWORK_TIMEOUT_MS.toInt()
        )
    }

    @Test
    fun `𝕄 upload proper request 𝕎 upload()`() {
        // Given
        mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{}")

        // When
        testedUploader.upload(
            mockWebServer.url("/").toString(),
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey,
            fakeIdentifier,
            fakeRepositoryInfo
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"}",
                "application/json; charset=utf-8"
            )
            .containsMultipartFile(
                "jvm_mapping_file",
                "jvm_mapping",
                fakeMappingFileContent,
                "text/plain"
            )
            .containsMultipartFile(
                "repository",
                "repository",
                fakeRepositoryFileContent,
                "application/json"
            )
    }

    @Test
    fun `𝕄 upload proper request 𝕎 upload() {repository=null}`() {
        // Given
        mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{}")

        // When
        testedUploader.upload(
            mockWebServer.url("/").toString(),
            fakeMappingFile,
            null,
            fakeApiKey,
            fakeIdentifier,
            null
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"}",
                "application/json; charset=utf-8"
            )
            .containsMultipartFile(
                "jvm_mapping_file",
                "jvm_mapping",
                fakeMappingFileContent,
                "text/plain"
            )
            .doesNotHaveField("repository")
            .doesNotHaveField("git_repository_url")
            .doesNotHaveField("git_commit_sha")
    }

    @Test
    fun `𝕄 throw exception 𝕎 upload() {response 403}`() {
        // Given
        mockResponse = MockResponse()
            .setResponseCode(403)
            .setBody("{}")

        // When
        assertThrows<IllegalStateException> {
            testedUploader.upload(
                mockWebServer.url("/").toString(),
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"}",
                "application/json; charset=utf-8"
            )
            .containsMultipartFile(
                "jvm_mapping_file",
                "jvm_mapping",
                fakeMappingFileContent,
                "text/plain"
            )
            .containsMultipartFile(
                "repository",
                "repository",
                fakeRepositoryFileContent,
                "application/json"
            )
    }

    @Test
    fun `𝕄 throw exception 𝕎 upload() {response 400-599}`(
        @IntForgery(400, 600) statusCode: Int
    ) {
        // 407 will actually throw a protocol exception
        // Received HTTP_PROXY_AUTH (407) code while not using proxy
        assumeTrue(statusCode != 407)

        // Given
        mockResponse = MockResponse()
            .setResponseCode(statusCode)
            .setBody("{}")

        // When
        assertThrows<IllegalStateException> {
            testedUploader.upload(
                mockWebServer.url("/").toString(),
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"}",
                "application/json; charset=utf-8"
            )
            .containsMultipartFile(
                "jvm_mapping_file",
                "jvm_mapping",
                fakeMappingFileContent,
                "text/plain"
            )
            .containsMultipartFile(
                "repository",
                "repository",
                fakeRepositoryFileContent,
                "application/json"
            )
    }

    @Test
    fun `M throw a MaxSizeExceededException W upload() { mappingFile size exceeded 50 MB}`(
        forge: Forge
    ) {
        // GIVEN
        val fakeFileSize =
            forge.aLong(min = OkHttpUploader.MAX_MAP_FILE_SIZE_IN_BYTES + 1)
        val fakeTooLargeMappingFile = spy(File(tempDir, forge.anAlphabeticalString()))
        doReturn(fakeFileSize)
            .whenever(fakeTooLargeMappingFile).length()

        // THEN
        assertThrows<MaxSizeExceededException> {
            testedUploader.upload(
                mockWebServer.url("/").toString(),
                fakeTooLargeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo
            )
        }
        assertThat(mockWebServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `𝕄 upload proper request 𝕎 upload() { mappingFile size smaller than 50 MB }`(
        forge: Forge
    ) {
        // Given
        val fakeFileSize =
            forge.aLong(min = 1, max = OkHttpUploader.MAX_MAP_FILE_SIZE_IN_BYTES)
        val fakeTooLargeMappingFile = spy(File(tempDir, forge.anAlphabeticalString()))
        doReturn(fakeFileSize)
            .whenever(fakeTooLargeMappingFile).length()
        mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{}")

        // When
        testedUploader.upload(
            mockWebServer.url("/").toString(),
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey,
            fakeIdentifier,
            fakeRepositoryInfo
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    @Test
    fun `𝕄 upload proper request 𝕎 upload() { mappingFile size is 50 MB }`(
        forge: Forge
    ) {
        // Given
        val fakeTooLargeMappingFile = spy(File(tempDir, forge.anAlphabeticalString()))
        doReturn(OkHttpUploader.MAX_MAP_FILE_SIZE_IN_BYTES)
            .whenever(fakeTooLargeMappingFile).length()
        mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{}")

        // When
        testedUploader.upload(
            mockWebServer.url("/").toString(),
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey,
            fakeIdentifier,
            fakeRepositoryInfo
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }

    inner class MockDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            dispatchedRequest = request
            return mockResponse
        }
    }
}
