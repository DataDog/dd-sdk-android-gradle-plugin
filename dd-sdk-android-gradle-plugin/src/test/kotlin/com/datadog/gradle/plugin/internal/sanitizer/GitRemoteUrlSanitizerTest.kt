/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.sanitizer

import com.datadog.gradle.plugin.Configurator
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.lang.RuntimeException
import java.util.Locale

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class GitRemoteUrlSanitizerTest {
    lateinit var testedUrlSanitizer: GitRemoteUrlSanitizer

    @BeforeEach
    fun `set up`() {
        testedUrlSanitizer = GitRemoteUrlSanitizer()
    }

    // region Tests

    @Test
    fun `M sanitize a HTTP url W sanitize()`(
        @StringForgery(regex = "http[s]?")
        fakeSchema: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            fakeSchema,
            fakePort,
            null,
            null,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(fakeRemoteUrl)
    }

    @Test
    fun `M sanitize a HTTP url W sanitize() { username and password provided }`(
        @StringForgery(regex = "http[s]?")
        fakeSchema: String,
        @StringForgery fakeUserName: String,
        @StringForgery fakePassword: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            fakeSchema,
            fakePort,
            fakeUserName,
            fakePassword,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(
            generateFakeFullRemoteUrl(
                fakeSchema,
                fakePort,
                null,
                null,
                fakeHost,
                fakeOwner,
                fakeRepository
            )
        )
    }

    @Test
    fun `M sanitize a HTTP url W sanitize() { username provided }`(
        @StringForgery(regex = "http[s]?")
        fakeSchema: String,
        @StringForgery fakeUserName: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            fakeSchema,
            fakePort,
            fakeUserName,
            null,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(
            generateFakeFullRemoteUrl(
                fakeSchema,
                fakePort,
                null,
                null,
                fakeHost,
                fakeOwner,
                fakeRepository
            )
        )
    }

    @Test
    fun `M sanitize a short format SSH url W sanitize()`(
        @StringForgery fakeUserName: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeShortFormatRemoteUrl(
            fakeUserName,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(fakeRemoteUrl)
    }

    @Test
    fun `M sanitize a SSH url W sanitize()`(
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            "ssh",
            fakePort,
            null,
            null,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(fakeRemoteUrl)
    }

    @Test
    fun `M sanitize a SSH url W sanitize() { username and password provided }`(
        @StringForgery fakeUserName: String,
        @StringForgery fakePassword: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            "ssh",
            fakePort,
            fakeUserName,
            fakePassword,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(
            generateFakeFullRemoteUrl(
                "ssh",
                fakePort,
                null,
                null,
                fakeHost,
                fakeOwner,
                fakeRepository
            )
        )
    }

    @Test
    fun `M sanitize a SSH url W sanitize() { username provided }`(
        @StringForgery fakeUserName: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            "ssh",
            fakePort,
            fakeUserName,
            null,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(
            generateFakeFullRemoteUrl(
                "ssh",
                fakePort,
                null,
                null,
                fakeHost,
                fakeOwner,
                fakeRepository
            )
        )
    }

    @Test
    fun `M sanitize a missing schema url W sanitize()`(
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            null,
            fakePort,
            null,
            null,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(fakeRemoteUrl)
    }

    @Test
    fun `M sanitize a missing schema url W sanitize() { username and password provided }`(
        @StringForgery fakeUserName: String,
        @StringForgery fakePassword: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            null,
            fakePort,
            fakeUserName,
            fakePassword,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(
            generateFakeFullRemoteUrl(
                null,
                fakePort,
                null,
                null,
                fakeHost,
                fakeOwner,
                fakeRepository
            )
        )
    }

    @Test
    fun `M sanitize a missing schema url W sanitize() { username provided }`(
        @StringForgery fakePassword: String,
        @StringForgery(
            regex = "[a-z0-9]+\\.[a-z]{3}"
        )
        fakeHost: String,
        @IntForgery(1, 99999) fakePort: Int,
        @StringForgery fakeOwner: String,
        @StringForgery fakeRepository: String
    ) {
        // Given
        val fakeRemoteUrl = generateFakeFullRemoteUrl(
            null,
            fakePort,
            null,
            fakePassword,
            fakeHost,
            fakeOwner,
            fakeRepository
        )

        // Then
        assertThat(testedUrlSanitizer.sanitize(fakeRemoteUrl)).isEqualTo(
            generateFakeFullRemoteUrl(
                null,
                fakePort,
                null,
                null,
                fakeHost,
                fakeOwner,
                fakeRepository
            )
        )
    }

    @Test
    fun `M throw a personal UriParsingException W sanitize() { strategy throws exception }`(
        @StringForgery(regex = "[a-z]+@github\\.com/[a-z]+/[a-z0-9_]+\\.git")
        fakeRemoteUrl: String,
        @StringForgery fakeExceptioMessage: String
    ) {
        // Given
        val fakeException = RuntimeException(fakeExceptioMessage)
        val mockedResolvedSanitizer: UrlSanitizer = mock()
        testedUrlSanitizer = GitRemoteUrlSanitizer(
            sanitizerResolver = {
                mockedResolvedSanitizer
            }
        )
        doThrow(fakeException).whenever(mockedResolvedSanitizer).sanitize(fakeRemoteUrl)

        // When
        val caughtException = catchThrowable {
            testedUrlSanitizer.sanitize(fakeRemoteUrl)
        }

        // Then
        assertThat(caughtException).isInstanceOf(UriParsingException::class.java)
        val expectedErrorMessage = GitRemoteUrlSanitizer.WRONG_URL_FORMAT_ERROR_MESSAGE.format(
            fakeRemoteUrl,
            Locale.US
        )
        assertThat(caughtException.message).startsWith(expectedErrorMessage)
        assertThat(caughtException.cause).isEqualTo(fakeException)
    }

    // endregion

    // region Internals

    private fun generateFakeFullRemoteUrl(
        schema: String?,
        port: Int,
        username: String?,
        password: String?,
        host: String,
        owner: String,
        repository: String
    ): String {
        val builder = StringBuilder()
        if (schema != null) {
            builder.append(schema)
            builder.append("://")
        }
        if (username != null) {
            builder.append(username)
            if (password != null) {
                builder.append(":")
                builder.append(username)
            }
            builder.append("@")
        }
        builder.append(host)
        builder.append(":")
        builder.append(port)
        builder.append("/")
        builder.append(owner)
        builder.append("/")
        builder.append(repository)
        builder.append(".git")
        return builder.toString()
    }

    private fun generateFakeShortFormatRemoteUrl(
        username: String?,
        host: String,
        owner: String,
        repository: String
    ): String {
        return "$username&$host/$owner/$repository.git"
    }
    // endregion
}
