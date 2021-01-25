package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class DdAndroidGradlePluginTest {

    lateinit var testedPlugin: DdAndroidGradlePlugin

    lateinit var fakeProject: Project

    @Mock
    lateinit var mockVariant: ApplicationVariant

    @Forgery
    lateinit var fakeExtension: DdExtension

    @StringForgery(StringForgeryType.HEXADECIMAL)
    lateinit var fakeApiKey: String

    @BeforeEach
    fun `set up`() {
        fakeProject = ProjectBuilder.builder().build()

        testedPlugin = DdAndroidGradlePlugin()
    }

    @Test
    fun `𝕄 configure the upload task with the variant info 𝕎 configureVariant()`(
        @StringForgery variantName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.versionName = null
        fakeExtension.serviceName = null
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName

        // When
        val task = testedPlugin.configureVariant(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.name).isEqualTo("uploadMapping${variantName.capitalize()}")
        assertThat(task.apiKey).isEqualTo(fakeApiKey)
        assertThat(task.variantName).isEqualTo(variantName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.envName).isEqualTo(fakeExtension.environmentName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.mappingFilePath).isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
    }


    @Test
    fun `𝕄 remove buildType 𝕎 configureVariant() {Debug variant}`(
        @StringForgery variantName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.versionName = null
        fakeExtension.serviceName = null
        val fullVariant = "${variantName}Debug"
        whenever(mockVariant.name) doReturn fullVariant
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName

        // When
        val task = testedPlugin.configureVariant(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.name).isEqualTo("uploadMapping${fullVariant.capitalize()}")
        assertThat(task.apiKey).isEqualTo(fakeApiKey)
        assertThat(task.variantName).isEqualTo(variantName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.envName).isEqualTo(fakeExtension.environmentName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.mappingFilePath).isEqualTo("${fakeProject.buildDir}/outputs/mapping/$fullVariant/mapping.txt")
    }


    @Test
    fun `𝕄 remove buildType 𝕎 configureVariant() {Release variant}`(
        @StringForgery variantName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.versionName = null
        fakeExtension.serviceName = null
        val fullVariant = "${variantName}Release"
        whenever(mockVariant.name) doReturn fullVariant
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName

        // When
        val task = testedPlugin.configureVariant(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.name).isEqualTo("uploadMapping${fullVariant.capitalize()}")
        assertThat(task.apiKey).isEqualTo(fakeApiKey)
        assertThat(task.variantName).isEqualTo(variantName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.envName).isEqualTo(fakeExtension.environmentName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.mappingFilePath).isEqualTo("${fakeProject.buildDir}/outputs/mapping/$fullVariant/mapping.txt")
    }
    @Test
    fun `𝕄 configure the upload task with the extension info 𝕎 configureVariant()`(
        @StringForgery variantName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName

        // When
        val task = testedPlugin.configureVariant(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.name).isEqualTo("uploadMapping${variantName.capitalize()}")
        assertThat(task.apiKey).isEqualTo(fakeApiKey)
        assertThat(task.variantName).isEqualTo(variantName)
        assertThat(task.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(task.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(task.envName).isEqualTo(fakeExtension.environmentName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.mappingFilePath).isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
    }
}