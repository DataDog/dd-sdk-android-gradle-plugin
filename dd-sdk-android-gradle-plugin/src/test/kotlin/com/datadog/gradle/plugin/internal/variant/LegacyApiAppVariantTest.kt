/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.variant

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.tasks.ExternalNativeBuildTask
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import com.android.builder.model.SourceProvider
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.File
import java.nio.file.Paths

@Extensions(
    ExtendWith(ForgeExtension::class),
    ExtendWith(MockitoExtension::class)
)
@ForgeConfiguration(value = Configurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class LegacyApiAppVariantTest {

    private lateinit var testedAppVariant: LegacyApiAppVariant

    private val fakeProject = ProjectBuilder.builder().build()

    @Mock
    lateinit var mockAppExtension: AppExtension

    @Mock
    lateinit var mockAndroidVariant: ApplicationVariant

    @BeforeEach
    fun `set up`() {
        testedAppVariant = LegacyApiAppVariant(
            mockAndroidVariant,
            mockAppExtension,
            fakeProject
        )
    }

    @Test
    fun `M return variant name W name`(
        @StringForgery fakeName: String
    ) {
        // Given
        whenever(mockAndroidVariant.name) doReturn fakeName

        // When
        val name = testedAppVariant.name

        // Then
        assertThat(name).isEqualTo(fakeName)
    }

    @Test
    fun `M return application ID W applicationId`(
        @StringForgery fakeApplicationId: String
    ) {
        // Given
        whenever(mockAndroidVariant.applicationId) doReturn fakeApplicationId

        // When
        val applicationId = testedAppVariant.applicationId.get()

        // Then
        assertThat(applicationId).isEqualTo(fakeApplicationId)
    }

    @Test
    fun `M return flavor name W flavorName`(
        @StringForgery fakeFlavorName: String
    ) {
        // Given
        whenever(mockAndroidVariant.flavorName) doReturn fakeFlavorName

        // When
        val flavorName = testedAppVariant.flavorName

        // Then
        assertThat(flavorName).isEqualTo(fakeFlavorName)
    }

    @Test
    fun `M return version name W versionName`(
        @StringForgery fakeVersionName: String
    ) {
        // Given
        whenever(mockAndroidVariant.versionName) doReturn fakeVersionName

        // When
        val versionName = testedAppVariant.versionName.get()

        // Then
        assertThat(versionName).isEqualTo(fakeVersionName)
    }

    @Test
    fun `M return empty version name W versionName { missing version name }`() {
        // Given
        whenever(mockAndroidVariant.versionName) doReturn null

        // When
        val versionName = testedAppVariant.versionName.get()

        // Then
        assertThat(versionName).isEmpty()
    }

    @Test
    fun `M return version code W versionCode`(
        @IntForgery(min = 1) fakeVersionCode: Int
    ) {
        // Given
        whenever(mockAndroidVariant.versionCode) doReturn fakeVersionCode

        // When
        val versionCode = testedAppVariant.versionCode.get()

        // Then
        assertThat(versionCode).isEqualTo(fakeVersionCode)
    }

    @Test
    fun `M return compile configuration W compileConfiguration`() {
        // Given
        val mockCompileConfiguration = mock<Configuration>()
        whenever(mockAndroidVariant.compileConfiguration) doReturn mockCompileConfiguration

        // When
        val compileConfiguration = testedAppVariant.compileConfiguration

        // Then
        assertThat(compileConfiguration).isSameAs(mockCompileConfiguration)
    }

    @Test
    fun `M return true W isNativeBuildEnabled { native build providers registered }`(
        forge: Forge
    ) {
        // Given
        val externalNativeBuildProviders = forge.aList { mock<TaskProvider<ExternalNativeBuildTask>>() }
        whenever(mockAndroidVariant.externalNativeBuildProviders) doReturn externalNativeBuildProviders

        // When
        val isNativeBuildEnabled = testedAppVariant.isNativeBuildEnabled

        // Then
        assertThat(isNativeBuildEnabled).isTrue()
    }

    @Test
    fun `M return false W isNativeBuildEnabled { native build providers not registered }`() {
        // Given
        whenever(mockAndroidVariant.externalNativeBuildProviders) doReturn emptyList()

        // When
        val isNativeBuildEnabled = testedAppVariant.isNativeBuildEnabled

        // Then
        assertThat(isNativeBuildEnabled).isFalse()
    }

    @Test
    fun `M return if minification is enabled or not W isMinifyEnabled`(
        @BoolForgery fakeMinifyEnabled: Boolean
    ) {
        // Given
        val mockBuildType = mock<BuildType>()
        whenever(mockBuildType.isMinifyEnabled) doReturn fakeMinifyEnabled
        whenever(mockAndroidVariant.buildType) doReturn mockBuildType

        // When
        val isMinifyEnabled = testedAppVariant.isMinifyEnabled

        // Then
        assertThat(isMinifyEnabled).isEqualTo(fakeMinifyEnabled)
    }

    @Test
    fun `M return build type name W buildTypeName`(
        @StringForgery fakeBuildTypeName: String
    ) {
        // Given
        val mockBuildType = mock<BuildType>()
        whenever(mockBuildType.name) doReturn fakeBuildTypeName
        whenever(mockAndroidVariant.buildType) doReturn mockBuildType

        // When
        val buildTypeName = testedAppVariant.buildTypeName

        // Then
        assertThat(buildTypeName).isEqualTo(fakeBuildTypeName)
    }

    @Test
    fun `M return flavor names W flavors`(
        @StringForgery fakeFlavorNames: List<String>
    ) {
        // Given
        val mockFlavors = fakeFlavorNames.map {
            val mockFlavor = mock<ProductFlavor>()
            whenever(mockFlavor.name) doReturn it
            mockFlavor
        }
        whenever(mockAndroidVariant.productFlavors) doReturn mockFlavors

        // When
        val flavors = testedAppVariant.flavors

        // Then
        assertThat(flavors).isEqualTo(fakeFlavorNames)
    }

    @Test
    fun `M return mapping file W mappingFile`(
        @StringForgery fakeVariantName: String
    ) {
        // Given
        whenever(mockAndroidVariant.name) doReturn fakeVariantName

        // When
        val mappingFile = testedAppVariant.mappingFile

        // Then
        assertThat(mappingFile.get().asFile.path).endsWith(
            Paths.get("outputs", "mapping", fakeVariantName, "mapping.txt").toString()
        )
    }

    @Test
    fun `M collect java and kotlin source dirs W collectJavaAndKotlinSourceDirectories`(
        @TempDir tempDir: File,
        forge: Forge
    ) {
        // Given
        val expectedSourceDirectories = mutableListOf<File>()
        val mockSourceSets = forge.aList {
            val mockSourceProvider = mock<SourceProvider>()
            val fakeJavaDirectories = aList { File(tempDir, anAlphaNumericalString()) }
            val fakeKotlinDirectories = aList { File(tempDir, anAlphaNumericalString()) }
            expectedSourceDirectories += fakeJavaDirectories
            expectedSourceDirectories += fakeKotlinDirectories
            whenever(mockSourceProvider.javaDirectories) doReturn fakeJavaDirectories
            whenever(mockSourceProvider.kotlinDirectories) doReturn fakeKotlinDirectories
            mockSourceProvider
        }
        whenever(mockAndroidVariant.sourceSets) doReturn mockSourceSets

        // When
        val sourceDirectories = testedAppVariant.collectJavaAndKotlinSourceDirectories()

        // Then
        assertThat(sourceDirectories.get())
            .isEqualTo(expectedSourceDirectories)
    }
}
