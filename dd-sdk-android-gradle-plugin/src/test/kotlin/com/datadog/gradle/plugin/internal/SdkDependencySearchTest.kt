/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class SdkDependencySearchTest {

    @StringForgery
    lateinit var fakeConfigurationName: String

    @TempDir
    lateinit var tempDir: File

    private lateinit var fakeProject: Project
    private lateinit var stubRootComponent: ResolvedComponentResult

    @BeforeEach
    fun `set up`(forge: Forge) {
        fakeProject = ProjectBuilder.builder().withProjectDir(tempDir).build()
        stubRootComponent = stubComponent(forge)
    }

    @Test
    fun `M return Present W search() { SDK is a direct dependency }`(forge: Forge) {
        // Given
        stubDependencies(stubRootComponent, stubSdkComponent(forge))

        // When
        val result = SdkDependencySearch.search(fakeProject.provider { stubRootComponent })

        // Then
        assertThat(result).isEqualTo(SdkSearchResult.Present)
    }

    @Test
    fun `M return Present W search() { SDK is a transitive dependency }`(forge: Forge) {
        // Given
        val stubParent = stubComponent(forge)
        stubDependencies(stubRootComponent, stubParent)
        stubDependencies(stubParent, stubSdkComponent(forge))

        // When
        val result = SdkDependencySearch.search(fakeProject.provider { stubRootComponent })

        // Then
        assertThat(result).isEqualTo(SdkSearchResult.Present)
    }

    @Test
    fun `M return Absent W search() { SDK not in dependencies }`(forge: Forge) {
        // Given
        stubDependencies(stubRootComponent, stubComponent(forge))

        // When
        val result = SdkDependencySearch.search(fakeProject.provider { stubRootComponent })

        // Then
        assertThat(result).isEqualTo(SdkSearchResult.Absent)
    }

    @Test
    fun `M return Absent W search() { cyclic dependencies without SDK }`(forge: Forge) {
        // Given
        val stubChild = stubComponent(forge)
        stubDependencies(stubRootComponent, stubChild)
        stubDependencies(stubChild, stubRootComponent)

        // When
        val result = SdkDependencySearch.search(fakeProject.provider { stubRootComponent })

        // Then
        assertThat(result).isEqualTo(SdkSearchResult.Absent)
    }

    @Test
    fun `M return Unresolved with causes W search() { unresolved transitive dependency }`(
        forge: Forge,
        @StringForgery fakeCauseMessage: String
    ) {
        // Given
        val stubChild = stubComponent(forge)
        val fakeException = fakeResolveException(fakeCauseMessage)
        val stubUnresolvedDependency = mock<UnresolvedDependencyResult> {
            on { failure } doReturn fakeException
        }
        stubDependencies(stubRootComponent, stubChild)
        whenever(stubChild.dependencies) doReturn setOf(stubUnresolvedDependency)

        // When
        val result = SdkDependencySearch.search(fakeProject.provider { stubRootComponent })

        // Then
        assertThat(result).isEqualTo(SdkSearchResult.Unresolved("${fakeException.message}\n$fakeCauseMessage"))
    }

    @Test
    fun `M return Present W search() { SDK found before an unresolved dependency }`(forge: Forge) {
        // Given
        val stubUnresolvedDependency = mock<UnresolvedDependencyResult> {
            on { failure } doReturn fakeResolveException(forge.anAlphabeticalString())
        }
        val stubSdk = stubComponent(forge, "com.datadoghq", "dd-sdk-android-core")
        stubDependencies(stubRootComponent, stubSdk)
        whenever(stubSdk.dependencies) doReturn setOf(stubUnresolvedDependency)

        // When
        val result = SdkDependencySearch.search(fakeProject.provider { stubRootComponent })

        // Then
        assertThat(result).isEqualTo(SdkSearchResult.Present)
    }

    @Test
    fun `M return Unresolved with causes W search() { graph resolution throws ResolveException }`(
        @StringForgery fakeCauseMessage: String
    ) {
        // Given
        val fakeException = fakeResolveException(fakeCauseMessage)
        val failingRootProvider = fakeProject.provider<ResolvedComponentResult> { throw fakeException }

        // When
        val result = SdkDependencySearch.search(failingRootProvider)

        // Then
        assertThat(result).isEqualTo(SdkSearchResult.Unresolved("${fakeException.message}\n$fakeCauseMessage"))
    }

    @Test
    fun `M propagate W search() { graph resolution throws non-ResolveException }`(
        @StringForgery fakeMessage: String
    ) {
        // Given
        val fakeException = IllegalStateException(fakeMessage)
        val failingRootProvider = fakeProject.provider<ResolvedComponentResult> { throw fakeException }

        // When / Then
        assertThatThrownBy { SdkDependencySearch.search(failingRootProvider) }.isSameAs(fakeException)
    }

    private fun fakeResolveException(causeMessage: String): ResolveException =
        object : ResolveException(fakeConfigurationName, listOf(IllegalStateException(causeMessage))) {}

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

    private fun stubSdkComponent(forge: Forge): ResolvedComponentResult =
        stubComponent(forge, "com.datadoghq", forge.anElementFrom("dd-sdk-android", "dd-sdk-android-core"))

    private fun stubDependencies(component: ResolvedComponentResult, vararg dependencies: ResolvedComponentResult) {
        val results = dependencies.map { stubDependency(it) }.toSet()
        whenever(component.dependencies) doReturn results
    }

    private fun stubDependency(component: ResolvedComponentResult): ResolvedDependencyResult = mock {
        on { selected } doReturn component
    }
}
