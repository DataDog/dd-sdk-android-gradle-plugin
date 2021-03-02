/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.Configurator
import com.datadog.gradle.plugin.RecordedRequestAssert.Companion.assertThat
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.File
import java.lang.IllegalStateException
import java.net.HttpURLConnection
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

    @StringForgery(regex = "[a-z]{8}\\.txt")
    lateinit var fakeMappingFileName: String

    @StringForgery(regex = "[a-z]{8}\\.txt")
    lateinit var fakeRepositoryFileName: String

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
        mockWebServer.setDispatcher(mockDispatcher)
        testedUploader = OkHttpUploader()
    }

    @AfterEach
    fun `tear down`() {
        mockWebServer.shutdown()
        dispatchedRequest = null
    }

    @Test
    fun `M use an OkHttpClient W callTimeout { 45 seconds }`() {
        assertThat(testedUploader.client.callTimeoutMillis()).isEqualTo(
            OkHttpUploader.NETWORK_TIMEOUT_MS.toInt()
        )
    }

    @Test
    fun `M use an OkHttpClient W writeTimeout { 45 seconds }`() {
        assertThat(testedUploader.client.writeTimeoutMillis()).isEqualTo(
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
            fakeIdentifier
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsFormData("version", fakeIdentifier.version)
            .containsFormData("service", fakeIdentifier.serviceName)
            .containsFormData("variant", fakeIdentifier.variant)
            .containsFormData("type", OkHttpUploader.TYPE_JVM_MAPPING_FILE)
            .containsMultipartFile(
                "jvm_mapping_file",
                fakeMappingFileName,
                fakeMappingFileContent,
                "text/plain"
            )
            .containsMultipartFile(
                "repository",
                fakeRepositoryFileName,
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
            fakeIdentifier
        )

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsFormData("version", fakeIdentifier.version)
            .containsFormData("service", fakeIdentifier.serviceName)
            .containsFormData("variant", fakeIdentifier.variant)
            .containsFormData("type", OkHttpUploader.TYPE_JVM_MAPPING_FILE)
            .containsMultipartFile(
                "jvm_mapping_file",
                fakeMappingFileName,
                fakeMappingFileContent,
                "text/plain"
            )
            .doesNotHaveField("repository")
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
                fakeIdentifier
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsFormData("version", fakeIdentifier.version)
            .containsFormData("service", fakeIdentifier.serviceName)
            .containsFormData("variant", fakeIdentifier.variant)
            .containsFormData("type", OkHttpUploader.TYPE_JVM_MAPPING_FILE)
            .containsMultipartFile(
                "jvm_mapping_file",
                fakeMappingFileName,
                fakeMappingFileContent,
                "text/plain"
            )
            .containsMultipartFile(
                "repository",
                fakeRepositoryFileName,
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
                fakeIdentifier
            )
        }

        // Then
        assertThat(mockWebServer.requestCount).isEqualTo(1)
        assertThat(dispatchedRequest)
            .hasMethod("POST")
            .containsFormData("version", fakeIdentifier.version)
            .containsFormData("service", fakeIdentifier.serviceName)
            .containsFormData("variant", fakeIdentifier.variant)
            .containsFormData("type", OkHttpUploader.TYPE_JVM_MAPPING_FILE)
            .containsMultipartFile(
                "jvm_mapping_file",
                fakeMappingFileName,
                fakeMappingFileContent,
                "text/plain"
            )
            .containsMultipartFile(
                "repository",
                fakeRepositoryFileName,
                fakeRepositoryFileContent,
                "application/json"
            )
    }

    inner class MockDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest?): MockResponse {
            dispatchedRequest = request
            return mockResponse
        }
    }
}
