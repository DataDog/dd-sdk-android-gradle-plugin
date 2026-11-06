/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import com.datadog.gradle.plugin.internal.SdkSearchResult
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File
import java.util.Locale

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class CheckSdkDepsTaskTest {
    lateinit var testedTask: CheckSdkDepsTask

    @Mock
    lateinit var mockLogger: Logger

    @StringForgery
    lateinit var fakeConfigurationName: String

    @StringForgery
    lateinit var fakeVariantName: String

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun `set up`(forge: Forge) {
        val fakeProject = ProjectBuilder.builder().withProjectDir(tempDir).build()
        testedTask = spy(
            fakeProject.tasks.register("checkDepsTask", CheckSdkDepsTask::class.java) {
                it.configurationName.set(fakeConfigurationName)
                it.sdkCheckLevel.set(forge.anElementFrom(SdkCheckLevel.WARN, SdkCheckLevel.FAIL))
                it.variantName.set(fakeVariantName)
            }.get()
        )
        whenever(testedTask.logger) doReturn mockLogger
    }

    @Test
    fun `M succeed W applyTask() { SDK present }`() {
        // Given
        testedTask.sdkSearchResult.set(SdkSearchResult.Present)

        // When
        assertDoesNotThrow { testedTask.applyTask() }

        // Then
        verifyNoInteractions(mockLogger)
    }

    @Test
    fun `M fail W applyTask() { SDK absent and level FAIL }`() {
        // Given
        testedTask.sdkSearchResult.set(SdkSearchResult.Absent)
        testedTask.sdkCheckLevel.set(SdkCheckLevel.FAIL)

        // When / Then
        assertThatThrownBy { testedTask.applyTask() }
            .isInstanceOf(MissingSdkException::class.java)
            .hasMessage(CheckSdkDepsTask.MISSING_DD_SDK_MESSAGE.format(Locale.US, fakeVariantName))
    }

    @Test
    fun `M warn W applyTask() { SDK absent and level WARN }`() {
        // Given
        testedTask.sdkSearchResult.set(SdkSearchResult.Absent)
        testedTask.sdkCheckLevel.set(SdkCheckLevel.WARN)

        // When
        testedTask.applyTask()

        // Then
        verify(mockLogger).warn(CheckSdkDepsTask.MISSING_DD_SDK_MESSAGE.format(Locale.US, fakeVariantName))
    }

    @Test
    fun `M warn without failing W applyTask() { unresolved dependencies, level FAIL }`(
        @StringForgery fakeMessage: String
    ) {
        // Given
        testedTask.sdkSearchResult.set(SdkSearchResult.Unresolved(fakeMessage))
        testedTask.sdkCheckLevel.set(SdkCheckLevel.FAIL)

        // When
        assertDoesNotThrow { testedTask.applyTask() }

        // Then
        verify(mockLogger).warn(
            CheckSdkDepsTask.CANNOT_RESOLVE_CONFIGURATION_MESSAGE.format(Locale.US, fakeConfigurationName, fakeMessage)
        )
        verifyNoMoreInteractions(mockLogger)
    }
}
