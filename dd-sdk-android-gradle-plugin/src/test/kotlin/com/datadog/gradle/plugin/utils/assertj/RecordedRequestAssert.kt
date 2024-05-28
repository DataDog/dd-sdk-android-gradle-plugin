/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.utils.assertj

import okhttp3.mockwebserver.RecordedRequest
import okio.GzipSource
import okio.buffer
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat

internal class RecordedRequestAssert(actual: RecordedRequest?) :
    AbstractObjectAssert<RecordedRequestAssert, RecordedRequest>(
        actual,
        RecordedRequestAssert::class.java
    ) {

    private val bodyContentUtf8 = if (actual?.getHeader("Content-Encoding") == "gzip") {
        val gzipSource = GzipSource(actual.body)
        gzipSource.buffer().readUtf8()
    } else {
        actual?.body?.readUtf8()
    }

    fun containsFormData(name: String, value: String): RecordedRequestAssert {
        isNotNull()
        assertThat(bodyContentUtf8)
            .contains(
                "Content-Disposition: form-data; name=\"$name\"\r\n" +
                    "Content-Length: ${value.length}\r\n\r\n$value\r\n"
            )
        return this
    }

    fun containsMultipartFile(
        name: String,
        fileName: String,
        fileContent: String,
        contentType: String
    ): RecordedRequestAssert {
        isNotNull()
        assertThat(bodyContentUtf8)
            .contains(
                "Content-Disposition: form-data; name=\"$name\"; filename=\"$fileName\"\r\n" +
                    "Content-Type: $contentType\r\n" +
                    "Content-Length: ${fileContent.length}\r\n\r\n$fileContent"
            )
        return this
    }

    fun doesNotHaveField(name: String): RecordedRequestAssert {
        isNotNull()
        assertThat(bodyContentUtf8)
            .doesNotContain(
                "Content-Disposition: form-data; name=\"$name\""
            )
        return this
    }

    fun hasMethod(expected: String): RecordedRequestAssert {
        isNotNull()
        assertThat(actual.method)
            .isEqualTo(expected)
        return this
    }

    fun hasHeaderValue(header: String, expected: String): RecordedRequestAssert {
        isNotNull
        assertThat(actual.headers.names()).contains(header)
        val actual = actual.getHeader(header)
        assertThat(actual).isEqualTo(expected)
        return this
    }

    fun doesNotHaveHeader(header: String): RecordedRequestAssert {
        isNotNull
        assertThat(actual.headers.names()).doesNotContain(header)
        return this
    }

    companion object {
        fun assertThat(actual: RecordedRequest?): RecordedRequestAssert {
            return RecordedRequestAssert(actual)
        }
    }
}
