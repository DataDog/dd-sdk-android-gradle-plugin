/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.variant

import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.ExternalNativeBuild
import com.android.build.api.variant.SourceDirectories
import com.android.build.api.variant.Sources
import com.android.build.api.variant.VariantOutput
import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.AdvancedForgery
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.MapForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
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
import java.util.Random

@Extensions(
    ExtendWith(ForgeExtension::class),
    ExtendWith(MockitoExtension::class)
)
@ForgeConfiguration(value = Configurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class NewApiAppVariantTest {

    private lateinit var testedAppVariant: NewApiAppVariant

    private val fakeProject = ProjectBuilder.builder().build()

    @Mock
    lateinit var mockAndroidVariant: ApplicationVariant

    @BeforeEach
    fun `set up`() {
        testedAppVariant = NewApiAppVariant(
            mockAndroidVariant,
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
        whenever(mockAndroidVariant.applicationId) doReturn fakeApplicationId.asProperty()

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
        @IntForgery(min = 1) fakeVersionCode: Int,
        @StringForgery fakeVersionName: String,
        forge: Forge
    ) {
        // Given
        val validOutput = mock<VariantOutput>()
        whenever(validOutput.enabled) doReturn true.asProperty()
        whenever(validOutput.versionCode) doReturn fakeVersionCode.asProperty()
        whenever(validOutput.versionName) doReturn fakeVersionName.asProperty()
        val fakeNonValidOutputs = forge.aList {
            val nonValidOutput = mock<VariantOutput>()
            if (forge.aBool()) {
                whenever(nonValidOutput.enabled) doReturn false.asProperty()
                whenever(nonValidOutput.versionCode) doReturn fakeVersionCode.asProperty()
            } else {
                whenever(nonValidOutput.enabled) doReturn true.asProperty()
                whenever(nonValidOutput.versionCode) doReturn null.asProperty()
            }
            nonValidOutput
        }
        val fakeVariantOutputs = (fakeNonValidOutputs + validOutput).shuffled(Random(forge.seed))
        whenever(mockAndroidVariant.outputs) doReturn fakeVariantOutputs

        // When
        val versionName = testedAppVariant.versionName.get()

        // Then
        assertThat(versionName).isEqualTo(fakeVersionName)
    }

    @Test
    fun `M return empty version name W versionName { missing version name }`(
        @IntForgery(min = 1) fakeVersionCode: Int,
        forge: Forge
    ) {
        // Given
        val validOutput = mock<VariantOutput>()
        whenever(validOutput.enabled) doReturn true.asProperty()
        whenever(validOutput.versionCode) doReturn fakeVersionCode.asProperty()
        whenever(validOutput.versionName) doReturn null.asProperty()
        val fakeNonValidOutputs = forge.aList {
            val nonValidOutput = mock<VariantOutput>()
            if (forge.aBool()) {
                whenever(nonValidOutput.enabled) doReturn false.asProperty()
                whenever(nonValidOutput.versionCode) doReturn fakeVersionCode.asProperty()
            } else {
                whenever(nonValidOutput.enabled) doReturn true.asProperty()
                whenever(nonValidOutput.versionCode) doReturn null.asProperty()
            }
            nonValidOutput
        }
        val fakeVariantOutputs = (fakeNonValidOutputs + validOutput).shuffled(Random(forge.seed))
        whenever(mockAndroidVariant.outputs) doReturn fakeVariantOutputs

        // When
        val versionName = testedAppVariant.versionName.get()

        // Then
        assertThat(versionName).isEmpty()
    }

    @Test
    fun `M return version code W versionCode`(
        @IntForgery(min = 1) fakeVersionCode: Int,
        forge: Forge
    ) {
        val validOutput = mock<VariantOutput>()
        whenever(validOutput.enabled) doReturn true.asProperty()
        whenever(validOutput.versionCode) doReturn fakeVersionCode.asProperty()
        val fakeNonValidOutputs = forge.aList {
            val nonValidOutput = mock<VariantOutput>()
            if (forge.aBool()) {
                whenever(nonValidOutput.enabled) doReturn false.asProperty()
                whenever(nonValidOutput.versionCode) doReturn fakeVersionCode.asProperty()
            } else {
                whenever(nonValidOutput.enabled) doReturn true.asProperty()
                whenever(nonValidOutput.versionCode) doReturn null.asProperty()
            }
            nonValidOutput
        }
        val fakeVariantOutputs = (fakeNonValidOutputs + validOutput).shuffled(Random(forge.seed))
        whenever(mockAndroidVariant.outputs) doReturn fakeVariantOutputs

        // When
        val versionCode = testedAppVariant.versionCode.get()

        // Then
        assertThat(versionCode).isEqualTo(fakeVersionCode)
    }

    @Test
    fun `M return default version code W versionCode { no valid variant output }`(
        @IntForgery(min = 1) fakeVersionCode: Int,
        forge: Forge
    ) {
        val fakeNonValidOutputs = forge.aList {
            val nonValidOutput = mock<VariantOutput>()
            if (forge.aBool()) {
                whenever(nonValidOutput.enabled) doReturn false.asProperty()
                whenever(nonValidOutput.versionCode) doReturn fakeVersionCode.asProperty()
            } else {
                whenever(nonValidOutput.enabled) doReturn true.asProperty()
                whenever(nonValidOutput.versionCode) doReturn null.asProperty()
            }
            nonValidOutput
        }
        whenever(mockAndroidVariant.outputs) doReturn fakeNonValidOutputs

        // When
        val versionCode = testedAppVariant.versionCode.get()

        // Then
        assertThat(versionCode).isEqualTo(1)
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
    fun `M return if native build is enabled W isNativeBuildEnabled`(
        @BoolForgery fakeEnabled: Boolean
    ) {
        // Given
        whenever(
            mockAndroidVariant.externalNativeBuild
        ) doReturn if (fakeEnabled) mock<ExternalNativeBuild>() else null

        // When
        val isNativeBuildEnabled = testedAppVariant.isNativeBuildEnabled

        // Then
        assertThat(isNativeBuildEnabled).isEqualTo(fakeEnabled)
    }

    @Test
    fun `M return if minification is enabled W isMinifyEnabled`(
        @BoolForgery fakeMinifyEnabled: Boolean
    ) {
        // Given
        @Suppress("UnstableApiUsage")
        whenever(mockAndroidVariant.isMinifyEnabled) doReturn fakeMinifyEnabled

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
        whenever(mockAndroidVariant.buildType) doReturn fakeBuildTypeName

        // When
        val buildTypeName = testedAppVariant.buildTypeName

        // Then
        assertThat(buildTypeName).isEqualTo(fakeBuildTypeName)
    }

    @Test
    fun `M return flavor names W flavors`(
        @MapForgery(
            key = AdvancedForgery(string = [StringForgery(StringForgeryType.ALPHABETICAL)]),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ALPHABETICAL)])
        ) fakeFlavors: Map<String, String>
    ) {
        // Given
        whenever(mockAndroidVariant.productFlavors) doReturn fakeFlavors.toList()

        // When
        val flavors = testedAppVariant.flavors

        // Then
        assertThat(flavors).isEqualTo(fakeFlavors.values.toList())
    }

    @Test
    fun `M return mapping file W mappingFile`(
        @StringForgery fakeMappingFileName: String
    ) {
        // Given
        val fakeMappingFile = File(fakeProject.layout.buildDirectory.get().asFile, fakeMappingFileName)
        val mockArtifacts = mock<Artifacts>()
        whenever(
            mockArtifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
        ) doReturn fakeProject.layout.buildDirectory.file(fakeMappingFile.path)
        whenever(mockAndroidVariant.artifacts) doReturn mockArtifacts

        // When
        val mappingFile = testedAppVariant.mappingFile

        // Then
        assertThat(mappingFile.get().asFile).isEqualTo(fakeMappingFile)
    }

    @RepeatedTest(4)
    fun `M collect java and kotlin source dirs W collectJavaAndKotlinSourceDirectories`(
        @TempDir tempDir: File,
        @BoolForgery includeJava: Boolean,
        @BoolForgery includeKotlin: Boolean,
        forge: Forge
    ) {
        // Given
        val expectedSourceDirectories = mutableListOf<File>()
        val mockSources = mock<Sources>()
        if (includeJava) {
            val mockJavaSources = mock<SourceDirectories.Flat>()
            val fakeJavaDirectories = forge.aList { File(tempDir, anAlphaNumericalString()) }
            whenever(
                mockJavaSources.all
            ) doReturn fakeJavaDirectories.map { it.asDirectory() }.asListProperty()
            whenever(mockSources.java) doReturn mockJavaSources
            expectedSourceDirectories += fakeJavaDirectories
        }
        if (includeKotlin) {
            val mockKotlinSources = mock<SourceDirectories.Flat>()
            val fakeKotlinDirectories = forge.aList { File(tempDir, anAlphaNumericalString()) }
            whenever(
                mockKotlinSources.all
            ) doReturn fakeKotlinDirectories.map { it.asDirectory() }.asListProperty()
            @Suppress("UnstableApiUsage")
            whenever(mockSources.kotlin) doReturn mockKotlinSources
            expectedSourceDirectories += fakeKotlinDirectories
        }
        whenever(mockAndroidVariant.sources) doReturn mockSources

        // When
        val sourceDirectories = testedAppVariant.collectJavaAndKotlinSourceDirectories()

        // Then
        assertThat(sourceDirectories.get())
            .isEqualTo(expectedSourceDirectories)
    }

    // region private

    private inline fun <reified T> T.asProperty(): Property<T> =
        fakeProject.objects.property(T::class.java).value(this)

    private inline fun <reified T> Collection<T>.asListProperty(): ListProperty<T> =
        fakeProject.objects.listProperty(T::class.java).value(this)

    private fun File.asDirectory(): Directory =
        fakeProject.objects.directoryProperty().fileValue(this).get()

    // endregion
}
