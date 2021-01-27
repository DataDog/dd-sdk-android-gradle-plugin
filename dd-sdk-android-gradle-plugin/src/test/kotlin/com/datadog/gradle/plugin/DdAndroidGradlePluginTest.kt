package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.datadog.gradle.plugin.internal.DdConfiguration
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.lang.IllegalStateException
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    // region configureVariant()

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
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
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
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$fullVariant/mapping.txt")
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
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$fullVariant/mapping.txt")
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
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
    }

    @Test
    fun `𝕄 use sensible defaults 𝕎 configureVariant() { empty config }`(
        @StringForgery variantName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.environmentName = null
        fakeExtension.serviceName = null
        fakeExtension.versionName = null
        fakeExtension.site = null
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
    }

    // endregion

    // region resolveApiKey

    @Test
    fun `𝕄 resolve API KEY from project properties 𝕎 resolveApiKey()`() {
        // Given
        fakeProject = mock()
        whenever(fakeProject.findProperty(DdAndroidGradlePlugin.DD_API_KEY)) doReturn fakeApiKey

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey).isEqualTo(fakeApiKey)
    }

    @Test
    fun `𝕄 resolve API KEY from environment variable 𝕎 resolveApiKey()`() {
        // Given
        setEnv(DdAndroidGradlePlugin.DD_API_KEY, fakeApiKey)

        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey).isEqualTo(fakeApiKey)
    }

    @Test
    fun `𝕄 throw exception 𝕎 resolveApiKey() {key not defined anywhere}`() {
        assertThrows<IllegalStateException> {
            testedPlugin.resolveApiKey(fakeProject)
        }
    }

    // endregion

    // region resolveExtensionConfiguration

    @Test
    fun `𝕄 return default configuration 𝕎 resolveExtensionConfiguration() { no variant config }`(
        @StringForgery flavorName: String
    ) {
        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, flavorName)

        // Then
        assertThat(config.environmentName).isEqualTo(fakeExtension.environmentName)
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
    }

    @Test
    fun `𝕄 return configuration 𝕎 resolveExtensionConfiguration() { variant config }`(
        @StringForgery flavorName: String,
        @Forgery variantConfig: DdExtensionConfiguration
    ) {
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants?.findByName(flavorName)) doReturn variantConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, flavorName)

        // Then
        assertThat(config.environmentName).isEqualTo(variantConfig.environmentName)
        assertThat(config.versionName).isEqualTo(variantConfig.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfig.serviceName)
        assertThat(config.site).isEqualTo(variantConfig.site)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w env only }`(
        @StringForgery flavorName: String,
        @StringForgery envName: String
    ) {
        val incompleteConfig = DdExtensionConfiguration().apply {
            environmentName = envName
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants?.findByName(flavorName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, flavorName)

        // Then
        assertThat(config.environmentName).isEqualTo(envName)
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w version only }`(
        @StringForgery flavorName: String,
        @StringForgery versionName: String
    ) {
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.versionName = versionName
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants?.findByName(flavorName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, flavorName)

        // Then
        assertThat(config.environmentName).isEqualTo(fakeExtension.environmentName)
        assertThat(config.versionName).isEqualTo(versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w service only }`(
        @StringForgery flavorName: String,
        @StringForgery serviceName: String
    ) {
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.serviceName = serviceName
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants?.findByName(flavorName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, flavorName)

        // Then
        assertThat(config.environmentName).isEqualTo(fakeExtension.environmentName)
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w site only }`(
        @StringForgery flavorName: String,
        @Forgery site: DdConfiguration.Site
    ) {
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.site = site.name
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants?.findByName(flavorName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, flavorName)

        // Then
        assertThat(config.environmentName).isEqualTo(fakeExtension.environmentName)
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(site.name)
    }

    // endregion
}
