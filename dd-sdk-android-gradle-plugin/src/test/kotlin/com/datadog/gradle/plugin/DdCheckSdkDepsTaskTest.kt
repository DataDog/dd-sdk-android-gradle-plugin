/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import com.datadog.gradle.plugin.utils.setStaticValue
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryException
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.slf4j.Logger
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.UncheckedIOException

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class DdCheckSdkDepsTaskTest {
    lateinit var testedTask: DdCheckSdkDepsTask

    lateinit var fakeSdkCheckLevel: SdkCheckLevel

    @Mock
    lateinit var mockConfiguration: Configuration

    @StringForgery
    lateinit var fakeConfigurationName: String

    @StringForgery
    lateinit var fakeVariantName: String

    @TempDir
    lateinit var tempDir: File

    @Mock
    lateinit var mockLogger: Logger

    lateinit var originalLogger: Logger

    @Mock
    lateinit var mockResolvedConfiguration: ResolvedConfiguration

    private lateinit var fakeProject: Project

    @BeforeEach
    fun `set up`(forge: Forge) {
        originalLogger = DdCheckSdkDepsTask.LOGGER
        DdCheckSdkDepsTask::class.java.setStaticValue("LOGGER", mockLogger)

        whenever(mockConfiguration.name).thenReturn(fakeConfigurationName)
        whenever(mockConfiguration.resolvedConfiguration).thenReturn(mockResolvedConfiguration)
        whenever(mockResolvedConfiguration.firstLevelModuleDependencies).thenReturn(emptySet())
        fakeSdkCheckLevel = forge.aValueFrom(SdkCheckLevel::class.java)
        fakeProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        fakeProject.configurations.add(mockConfiguration)

        testedTask = fakeProject.tasks.create(
            "DdCheckDepsTask",
            DdCheckSdkDepsTask::class.java
        ) {
            it.configurationName.set(fakeConfigurationName)
            it.sdkCheckLevel.set(fakeSdkCheckLevel)
            it.variantName.set(fakeVariantName)
        }
    }

    @AfterEach
    fun `tear down`() {
        DdCheckSdkDepsTask::class.java.setStaticValue("LOGGER", originalLogger)
    }

    // region taskAction

    @Test
    fun `𝕄 log info + exception 𝕎 configuration cannot be found`() {
        // GIVEN
        fakeProject.configurations.remove(mockConfiguration)

        // WHEN
        testedTask.applyTask()

        // THEN
        verify(mockLogger).info(
            DdCheckSdkDepsTask.CANNOT_FIND_CONFIGURATION_MESSAGE
                .format(fakeConfigurationName, fakeVariantName)
        )
    }

    @Test
    fun `𝕄 log info + exception 𝕎 configuration cannot be resolved`(
        forge: Forge
    ) {
        // GIVEN
        whenever(mockResolvedConfiguration.hasError()).thenReturn(true)
        val resolveException = ResolveException(forge.anAlphaNumericalString(), forge.anException())
        whenever(mockResolvedConfiguration.rethrowFailure()).thenThrow(resolveException)

        // WHEN
        testedTask.applyTask()

        // THEN
        verify(mockLogger).warn(anyString(), eq(resolveException))
    }

    @Test
    fun `𝕄 throw exception 𝕎 resolved configuration throws non-ResolveException`(
        forge: Forge
    ) {
        // GIVEN
        whenever(mockResolvedConfiguration.hasError()).thenReturn(true)
        val exception = forge.anException()
        whenever(mockResolvedConfiguration.rethrowFailure()).thenThrow(exception)

        // WHEN + THEN
        assertThatThrownBy { testedTask.applyTask() }
            .isEqualTo(exception)
    }

    @Test
    fun `𝕄 throw exception 𝕎 rethrowFailure doesn't throw`() {
        // GIVEN
        whenever(mockResolvedConfiguration.hasError()).thenReturn(true)

        // WHEN + THEN
        assertThatThrownBy { testedTask.applyTask() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `𝕄 throw exception 𝕎 sdk dependency could not be found { sdkCheckLevel = FAIL }`(
        forge: Forge
    ) {
        // GIVEN
        val dependencies = forge.aList {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }
        whenever(mockResolvedConfiguration.firstLevelModuleDependencies).thenReturn(
            dependencies.toSet()
        )
        testedTask.sdkCheckLevel.set(SdkCheckLevel.FAIL)

        // WHEN + THEN
        assertThatThrownBy { testedTask.applyTask() }.isInstanceOf(MissingSdkException::class.java)
        assertThat(testedTask.isLastRunSuccessful).isFalse()
    }

    @Test
    fun `𝕄 log a warning 𝕎 sdk dependency could not be found { sdkCheckLevel = WARN }`(
        forge: Forge
    ) {
        // GIVEN
        val dependencies = forge.aList {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }
        whenever(mockResolvedConfiguration.firstLevelModuleDependencies).thenReturn(
            dependencies.toSet()
        )
        testedTask.sdkCheckLevel.set(SdkCheckLevel.WARN)

        // WHEN
        testedTask.applyTask()

        // THEN
        verify(mockLogger).warn(DdCheckSdkDepsTask.MISSING_DD_SDK_MESSAGE.format(fakeVariantName))
        assertThat(testedTask.isLastRunSuccessful).isFalse()
    }

    @Test
    fun `𝕄 do nothing 𝕎 sdk dependency was found`(
        forge: Forge
    ) {
        // GIVEN
        val dependencies = forge.aList {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn "dd-sdk-android"
            whenever(dependency.moduleGroup) doReturn "com.datadoghq"
            dependency
        }
        whenever(mockResolvedConfiguration.firstLevelModuleDependencies).thenReturn(
            dependencies.toSet()
        )
        testedTask.sdkCheckLevel.set(
            forge.aValueFrom(
                SdkCheckLevel::class.java,
                exclude = listOf(SdkCheckLevel.NONE)
            )
        )

        // WHEN
        testedTask.applyTask()

        // THEN
        verifyNoInteractions(mockLogger)
        assertThat(testedTask.isLastRunSuccessful).isTrue()
    }

    // endregion

    // region isDatadogDependencyPresent

    @Test
    fun `𝕄 return true 𝕎 isDatadogDependencyPresent() { sdk is at the top level }`(
        forge: Forge
    ) {
        // GIVEN
        val sdkDependency = mock<ResolvedDependency>()
        whenever(sdkDependency.moduleName) doReturn "dd-sdk-android"
        whenever(sdkDependency.moduleGroup) doReturn "com.datadoghq"

        val otherDependencies = forge.aList {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }

        val allDependencies = (otherDependencies + sdkDependency).shuffled().toSet()

        // WHEN + THEN
        assertThat(
            testedTask.isDatadogDependencyPresent(allDependencies)
        ).isTrue()
    }

    @Test
    fun `𝕄 return true 𝕎 isDatadogDependencyPresent() { sdk is at the child level }`(
        forge: Forge
    ) {
        // GIVEN
        val sdkDependency = mock<ResolvedDependency>()
        whenever(sdkDependency.moduleName) doReturn "dd-sdk-android"
        whenever(sdkDependency.moduleGroup) doReturn "com.datadoghq"

        val fakeDependencyGenerator: Forge.() -> ResolvedDependency = {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }

        val childDependencies = forge.aList(forging = fakeDependencyGenerator)
        val topDependencies = forge.aList(forging = fakeDependencyGenerator)
        whenever(topDependencies.random().children) doReturn (childDependencies + sdkDependency)
            .shuffled().toSet()

        // WHEN + THEN
        assertThat(
            testedTask.isDatadogDependencyPresent(topDependencies.toSet())
        ).isTrue()
    }

    @Test
    fun `𝕄 return false 𝕎 isDatadogDependencyPresent() { sdk is not there }`(
        forge: Forge
    ) {
        // GIVEN
        val fakeDependencyGenerator: Forge.() -> ResolvedDependency = {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }

        val childDependencies = forge.aList(forging = fakeDependencyGenerator)
        val topDependencies = forge.aList(forging = fakeDependencyGenerator)

        whenever(topDependencies.random().children) doReturn childDependencies.toSet()

        // WHEN + THEN
        assertThat(
            testedTask.isDatadogDependencyPresent(topDependencies.toSet())
        ).isFalse()
    }

    // endregion

    // region private

    private fun Forge.anException(): Exception {
        val errorMessage = anAlphabeticalString()
        return anElementFrom(
            IndexOutOfBoundsException(errorMessage),
            ArithmeticException(errorMessage),
            IllegalStateException(errorMessage),
            ArrayIndexOutOfBoundsException(errorMessage),
            NullPointerException(errorMessage),
            ForgeryException(errorMessage),
            UnsupportedOperationException(errorMessage),
            SecurityException(errorMessage),
            UncheckedIOException(IOException(errorMessage)),
            UncheckedIOException(FileNotFoundException(errorMessage)),
            UncheckedIOException(EOFException(errorMessage))
        )
    }

    // endregion
}
