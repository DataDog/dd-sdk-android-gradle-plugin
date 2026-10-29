/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import com.datadog.gradle.plugin.internal.SdkDependencyGraph
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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

    private lateinit var fakeProject: Project
    private lateinit var stubRootComponent: ResolvedComponentResult

    @BeforeEach
    fun `set up`(forge: Forge) {
        fakeProject = ProjectBuilder.builder().withProjectDir(tempDir).build()
        stubRootComponent = stubComponent(forge)
        val stubRootProvider = fakeProject.provider { stubRootComponent }
        testedTask = spy(
            fakeProject.tasks.register("checkDepsTask", CheckSdkDepsTask::class.java) {
                it.configurationName.set(fakeConfigurationName)
                it.sdkCheckLevel.set(forge.anElementFrom(SdkCheckLevel.WARN, SdkCheckLevel.FAIL))
                it.variantName.set(fakeVariantName)
                it.dependencyGraph.set(fakeProject.provider { SdkDependencyGraph.resolve(stubRootProvider) })
            }.get()
        )
        whenever(testedTask.logger) doReturn mockLogger
    }

    @Test
    fun `M log configuration name W applyTask() { dependency graph absent }`() {
        // Given
        testedTask.dependencyGraph.unset()

        // When
        testedTask.applyTask()

        // Then
        verify(mockLogger).info(
            CheckSdkDepsTask.CANNOT_FIND_CONFIGURATION_MESSAGE
                .format(Locale.US, fakeConfigurationName, fakeVariantName)
        )
    }

    @Test
    fun `M log resolution failure W applyTask() { unresolved transitive dependency }`(forge: Forge) {
        // Given
        val stubSdk = stubComponent(forge, "com.datadoghq", "dd-sdk-android-core")
        val fakeException = object : ResolveException(fakeConfigurationName, emptyList<Throwable>()) {}
        val stubUnresolvedDependency = mock<UnresolvedDependencyResult> {
            on { failure } doReturn fakeException
        }
        stubDependencies(stubRootComponent, stubSdk)
        whenever(stubSdk.dependencies) doReturn setOf(stubUnresolvedDependency)
        testedTask.sdkCheckLevel.set(SdkCheckLevel.FAIL)

        // When
        testedTask.applyTask()

        // Then
        verify(mockLogger).warn("Couldn't resolve configuration $fakeConfigurationName", fakeException)
    }

    @Test
    fun `M fail W applyTask() { SDK absent and level FAIL }`(forge: Forge) {
        // Given
        stubDependencies(stubRootComponent, stubComponent(forge))
        testedTask.sdkCheckLevel.set(SdkCheckLevel.FAIL)

        // When / Then
        assertThatThrownBy { testedTask.applyTask() }
            .isInstanceOf(MissingSdkException::class.java)
            .hasMessage(CheckSdkDepsTask.MISSING_DD_SDK_MESSAGE.format(Locale.US, fakeVariantName))
    }

    @Test
    fun `M warn W applyTask() { SDK absent and level WARN }`(forge: Forge) {
        // Given
        stubDependencies(stubRootComponent, stubComponent(forge))
        testedTask.sdkCheckLevel.set(SdkCheckLevel.WARN)

        // When
        testedTask.applyTask()

        // Then
        verify(mockLogger).warn(CheckSdkDepsTask.MISSING_DD_SDK_MESSAGE.format(Locale.US, fakeVariantName))
    }

    @ParameterizedTest
    @CsvSource("dd-sdk-android,false", "dd-sdk-android,true", "dd-sdk-android-core,false", "dd-sdk-android-core,true")
    fun `M succeed W applyTask() { SDK present }`(sdkModuleName: String, transitive: Boolean, forge: Forge) {
        // Given
        val stubSdk = stubComponent(forge, "com.datadoghq", sdkModuleName)
        val stubParent = if (transitive) stubComponent(forge) else stubRootComponent
        stubDependencies(stubRootComponent, stubParent)
        stubDependencies(stubParent, stubSdk)

        // When
        assertDoesNotThrow { testedTask.applyTask() }

        // Then
        verifyNoInteractions(mockLogger)
    }

    @Test
    fun `M finish traversal W applyTask() { cyclic dependencies without SDK }`(forge: Forge) {
        // Given
        val stubChild = stubComponent(forge)
        stubDependencies(stubRootComponent, stubChild)
        stubDependencies(stubChild, stubRootComponent)
        testedTask.sdkCheckLevel.set(SdkCheckLevel.WARN)

        // When
        testedTask.applyTask()

        // Then
        verify(mockLogger).warn(CheckSdkDepsTask.MISSING_DD_SDK_MESSAGE.format(Locale.US, fakeVariantName))
    }

    private fun stubComponent(
        forge: Forge,
        group: String = forge.anAlphabeticalString(),
        name: String = forge.anAlphabeticalString()
    ): ResolvedComponentResult {
        val stubModule = mock<ModuleVersionIdentifier> {
            on { this.group } doReturn group
            on { this.name } doReturn name
        }
        val stubId = mock<ComponentIdentifier>()
        return mock {
            on { id } doReturn stubId
            on { moduleVersion } doReturn stubModule
            on { dependencies } doReturn emptySet()
        }
    }

    private fun stubDependencies(component: ResolvedComponentResult, vararg dependencies: ResolvedComponentResult) {
        val results = dependencies.map { stubDependency(it) }.toSet()
        whenever(component.dependencies) doReturn results
    }

    private fun stubDependency(component: ResolvedComponentResult): ResolvedDependencyResult = mock {
        on { selected } doReturn component
    }
}
