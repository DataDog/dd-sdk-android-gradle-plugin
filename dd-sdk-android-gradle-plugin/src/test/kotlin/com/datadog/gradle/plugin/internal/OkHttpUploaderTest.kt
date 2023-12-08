/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.Configurator
import com.datadog.gradle.plugin.DatadogSite
import com.datadog.gradle.plugin.RecordedRequestAssert.Companion.assertThat
import com.datadog.gradle.plugin.RepositoryInfo
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
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File
import java.net.HttpURLConnection
import java.util.Locale
import kotlin.IllegalStateException

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

    @Mock
    lateinit var mockSite: DatadogSite

    lateinit var mockWebServer: MockWebServer

    lateinit var mockUploadResponse: MockResponse
    lateinit var mockApiKeyValidationResponse: MockResponse

    lateinit var mockDispatcher: Dispatcher

    lateinit var fakeUploadUrl: String
    lateinit var fakeApiKeyValidationUrl: String

    var dispatchedUploadRequest: RecordedRequest? = null
    var dispatchedApiKeyValidationRequest: RecordedRequest? = null

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

        fakeUploadUrl = mockWebServer.url("/upload").toString()
        fakeApiKeyValidationUrl = mockWebServer.url("/api-key-validation").toString()

        whenever(mockSite.uploadEndpoint()) doReturn fakeUploadUrl
        whenever(mockSite.apiKeyVerificationEndpoint()) doReturn fakeApiKeyValidationUrl
    }

    @AfterEach
    fun `tear down`() {
        mockWebServer.shutdown()
        dispatchedUploadRequest = null
        dispatchedApiKeyValidationRequest = null
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
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{}")

        // When
        testedUploader.upload(
            mockSite,
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey,
            fakeIdentifier,
            fakeRepositoryInfo,
            useGzip = true
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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
    fun `𝕄 upload proper request 𝕎 upload() { without gzip }`() {
        // Given
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{}")

        // When
        testedUploader.upload(
            mockSite,
            fakeMappingFile,
            fakeRepositoryFile,
            fakeApiKey,
            fakeIdentifier,
            fakeRepositoryInfo,
            useGzip = false
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .doesNotHaveHeader("Content-Encoding")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{}")

        // When
        testedUploader.upload(
            mockSite,
            fakeMappingFile,
            null,
            fakeApiKey,
            fakeIdentifier,
            null,
            useGzip = true
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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
        mockUploadResponse = MockResponse()
            .setResponseCode(403)
            .setBody("{}")

        // When
        assertThrows<OkHttpUploader.InvalidApiKeyException> {
            testedUploader.upload(
                mockSite,
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo,
                useGzip = true
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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
    fun `𝕄 throw exception 𝕎 upload() {response 401-599}`(
        @IntForgery(401, 600) statusCode: Int
    ) {
        // 407 will actually throw a protocol exception
        // Received HTTP_PROXY_AUTH (407) code while not using proxy
        assumeTrue(statusCode != 407 && statusCode != 403)

        // Given
        mockUploadResponse = MockResponse()
            .setResponseCode(statusCode)
            .setBody("{}")

        // When
        assertThrows<IllegalStateException> {
            testedUploader.upload(
                mockSite,
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo,
                useGzip = true
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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
    fun `𝕄 throw generic exception 𝕎 upload() {response 400, API key validation returned true}`() {
        // Given
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody("{}")
        mockApiKeyValidationResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{ \"valid\": true }")

        // When
        assertThrows<IllegalStateException> {
            testedUploader.upload(
                mockSite,
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo,
                useGzip = true
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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

        assertThat(dispatchedApiKeyValidationRequest)
            .hasHeaderValue("DD-API-KEY", fakeApiKey)
    }

    @RepeatedTest(8)
    fun `𝕄 throw generic exception 𝕎 upload() {response 400, API key validation failed}`(
        forge: Forge
    ) {
        // Given
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody("{}")
        mockApiKeyValidationResponse = forge.anElementFrom(
            // wrong body
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("{}"),
            // no body
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK),
            // unexpected code
            MockResponse()
                .setResponseCode(forge.anInt(min = 400, max = 403)),
            MockResponse()
                .setResponseCode(forge.anInt(min = 404, max = 600))
        )

        // When
        assertThrows<IllegalStateException> {
            testedUploader.upload(
                mockSite,
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo,
                useGzip = true
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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

        assertThat(dispatchedApiKeyValidationRequest)
            .hasHeaderValue("DD-API-KEY", fakeApiKey)
    }

    @Test
    fun `𝕄 throw API key validation exception 𝕎 upload() {response 400, API key validation returned false}`() {
        // Given
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody("{}")

        mockApiKeyValidationResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("{ \"valid\": false }")

        // When
        assertThrows<OkHttpUploader.InvalidApiKeyException> {
            testedUploader.upload(
                mockSite,
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo,
                useGzip = true
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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

        assertThat(dispatchedApiKeyValidationRequest)
            .hasHeaderValue("DD-API-KEY", fakeApiKey)
    }

    @Test
    fun `𝕄 throw API key validation exception 𝕎 upload() {response 400, API key validation returned 403}`() {
        // Given
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody("{}")

        mockApiKeyValidationResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_FORBIDDEN)

        // When
        assertThrows<OkHttpUploader.InvalidApiKeyException> {
            testedUploader.upload(
                mockSite,
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo,
                useGzip = true
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(2)
        assertThat(dispatchedUploadRequest)
            .hasMethod("POST")
            .hasHeaderValue("Content-Encoding", "gzip")
            .containsFormData("git_repository_url", fakeRepositoryInfo.url)
            .containsFormData("git_commit_sha", fakeRepositoryInfo.hash)
            .containsMultipartFile(
                "event",
                "event",
                "{\"service\":\"${fakeIdentifier.serviceName}\"," +
                    "\"variant\":\"${fakeIdentifier.variant}\"," +
                    "\"buildId\":\"${fakeIdentifier.buildId}\"," +
                    "\"type\":\"${OkHttpUploader.TYPE_JVM_MAPPING_FILE}\"," +
                    "\"version\":\"${fakeIdentifier.version}\"," +
                    "\"versionCode\":${fakeIdentifier.versionCode}}",
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

        assertThat(dispatchedApiKeyValidationRequest)
            .hasHeaderValue("DD-API-KEY", fakeApiKey)
    }

    @Test
    fun `M throw a MaxSizeExceededException W upload() { response 413 }`() {
        // GIVEN
        mockUploadResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_ENTITY_TOO_LARGE)
            .setBody("{}")

        // THEN
        val exception = assertThrows<MaxSizeExceededException> {
            testedUploader.upload(
                mockSite,
                fakeMappingFile,
                fakeRepositoryFile,
                fakeApiKey,
                fakeIdentifier,
                fakeRepositoryInfo,
                useGzip = true
            )
        }
        assertThat(exception.message).isEqualTo(
            OkHttpUploader.MAX_MAP_SIZE_EXCEEDED_ERROR.format(Locale.US, fakeIdentifier)
        )
    }

    inner class MockDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return when (request.requestUrl?.encodedPath) {
                "/upload" -> {
                    dispatchedUploadRequest = request
                    mockUploadResponse
                }
                "/api-key-validation" -> {
                    dispatchedApiKeyValidationRequest = request
                    mockApiKeyValidationResponse
                }
                else -> error("Unexpected path for url=${request.requestUrl}")
            }
        }
    }
}
