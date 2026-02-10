/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.ApiKeySource
import com.datadog.gradle.plugin.internal.variant.AppVariant
import com.datadog.gradle.plugin.utils.capitalizeChar
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File
import java.util.UUID

/**
 * Base class for DdAndroidGradlePlugin tests.
 * Provides shared setup, mocks, and helper methods.
 */
@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal abstract class DdAndroidGradlePluginTestBase {

    lateinit var testedPlugin: DdAndroidGradlePlugin

    lateinit var fakeProject: Project

    @Mock
    lateinit var mockVariant: AppVariant

    lateinit var fakeBuildId: String

    @Forgery
    lateinit var fakeExtension: DdExtension

    lateinit var fakeApiKey: ApiKey

    @StringForgery(case = Case.LOWER)
    lateinit var fakeFlavorNames: List<String>

    @StringForgery(regex = "debug|preRelease|release")
    lateinit var fakeBuildTypeName: String

    @BeforeEach
    fun `set up`(forge: Forge) {
        fakeApiKey = ApiKey(
            value = forge.anHexadecimalString(),
            source = forge.aValueFrom(ApiKeySource::class.java)
        )
        fakeFlavorNames = fakeFlavorNames.take(5) // A D F G A♭ A A♭ G F
        fakeBuildId = forge.getForgery<UUID>().toString()
        fakeProject = ProjectBuilder.builder().build()
        testedPlugin = DdAndroidGradlePlugin(
            execOps = mock(),
            providerFactory = fakeProject.providers
        )

        setEnv(DdAndroidGradlePlugin.DD_API_KEY, "")
        setEnv(DdAndroidGradlePlugin.DATADOG_API_KEY, "")
    }

    // region Helper Methods

    protected fun List<String>.variantName(): String {
        return first() + drop(1).joinToString("") { it.replaceFirstChar { capitalizeChar(it) } }
    }

    protected fun mockBuildIdGenerationTask(buildId: String): TaskProvider<GenerateBuildIdTask> {
        return mock<TaskProvider<GenerateBuildIdTask>>().apply {
            whenever(
                flatMap(any<Transformer<Provider<String>, GenerateBuildIdTask>>())
            ) doReturn buildId.asProvider()
        }
    }

    protected fun <T> T.asProvider(): Provider<T> {
        return fakeProject.provider { this }
    }

    protected fun String.asFileProvider(): Provider<RegularFile> {
        return fakeProject.objects.fileProperty().value { File(this) }
    }

    // endregion
}
