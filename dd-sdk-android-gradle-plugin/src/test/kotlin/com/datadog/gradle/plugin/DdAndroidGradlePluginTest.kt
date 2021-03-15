package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import com.datadog.gradle.plugin.internal.DdConfiguration
import com.datadog.gradle.plugin.internal.GitRepositoryDetector
import com.datadog.gradle.plugin.internal.MissingSdkException
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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

    @Mock
    lateinit var mockBuildType: BuildType

    @Forgery
    lateinit var fakeExtension: DdExtension

    @StringForgery(StringForgeryType.HEXADECIMAL)
    lateinit var fakeApiKey: String

    @StringForgery(case = Case.LOWER)
    lateinit var fakeFlavorNames: List<String>

    @StringForgery(regex = "debug|preRelease|release")
    lateinit var fakeBuildTypeName: String

    @BeforeEach
    fun `set up`() {
        fakeFlavorNames = fakeFlavorNames.take(5) // A D F G A♭ A A♭ G F
        fakeProject = ProjectBuilder.builder().build()
        testedPlugin = DdAndroidGradlePlugin()
    }

    // region configureVariant()

    @Test
    fun `𝕄 configure the upload task with the variant info 𝕎 configureVariant()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.versionName = null
        fakeExtension.serviceName = null
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo("uploadMapping${variantName.capitalize()}")
        assertThat(task.apiKey).isEqualTo(fakeApiKey)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
    }

    @Test
    fun `𝕄 configure the upload task with the extension info 𝕎 configureVariant()`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo("uploadMapping${variantName.capitalize()}")
        assertThat(task.apiKey).isEqualTo(fakeApiKey)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(task.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(task.site).isEqualTo(fakeExtension.site)
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
    }

    @Test
    fun `𝕄 use sensible defaults 𝕎 configureVariant() { empty config }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.serviceName = null
        fakeExtension.versionName = null
        fakeExtension.site = null
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        check(task is DdMappingFileUploadTask)
        assertThat(task.repositoryDetector).isInstanceOf(GitRepositoryDetector::class.java)
        assertThat(task.name).isEqualTo("uploadMapping${variantName.capitalize()}")
        assertThat(task.apiKey).isEqualTo(fakeApiKey)
        assertThat(task.variantName).isEqualTo(flavorName)
        assertThat(task.versionName).isEqualTo(versionName)
        assertThat(task.serviceName).isEqualTo(packageName)
        assertThat(task.site).isEqualTo("")
        assertThat(task.mappingFilePath)
            .isEqualTo("${fakeProject.buildDir}/outputs/mapping/$variantName/mapping.txt")
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariant() {minify disabled}`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn false
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        assertThat(task).isNull()
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariant() {plugin disabled}`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.enabled = false
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.isMinifyEnabled) doReturn true
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        // When
        val task = testedPlugin.configureVariantForUploadTask(
            fakeProject,
            mockVariant,
            fakeApiKey,
            fakeExtension
        )

        // Then
        assertThat(task).isNull()
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
    fun `𝕄 returns empty String 𝕎 resolveApiKey() {key not defined anywhere}`() {
        // When
        val apiKey = testedPlugin.resolveApiKey(fakeProject)

        // Then
        assertThat(apiKey).isEmpty()
    }

    // endregion

    // region resolveExtensionConfiguration

    @Test
    fun `𝕄 return default config 𝕎 resolveExtensionConfiguration() {no variant config}`() {
        // When
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
    }

    @Test
    fun `𝕄 return config 𝕎 resolveExtensionConfiguration() { variant w full config }`(
        @Forgery variantConfig: DdExtensionConfiguration
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(variantName)) doReturn variantConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfig.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfig.serviceName)
        assertThat(config.site).isEqualTo(variantConfig.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfig.checkProjectDependencies)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w version only }`(
        @StringForgery versionName: String
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.versionName = versionName
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w service only }`(
        @StringForgery serviceName: String
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.serviceName = serviceName
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w site only }`(
        @Forgery site: DdConfiguration.Site
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.site = site.name
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(site.name)
        assertThat(config.checkProjectDependencies).isEqualTo(
            fakeExtension.checkProjectDependencies
        )
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w sdkCheck only }`(
        @Forgery sdkCheckLevel: SdkCheckLevel
    ) {
        val variantName = fakeFlavorNames.variantName()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.checkProjectDependencies = sdkCheckLevel
        }
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.checkProjectDependencies).isEqualTo(sdkCheckLevel)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { simple variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigA: DdExtensionConfiguration,
        @Forgery variantConfigB: DdExtensionConfiguration,
        @Forgery variantConfigC: DdExtensionConfiguration
    ) {
        val flavorNames = listOf(flavorA, flavorB, flavorC)
        variantConfigA.apply {
            versionName = null
            checkProjectDependencies = null
        }
        variantConfigB.apply {
            serviceName = null
            checkProjectDependencies = null
        }
        variantConfigC.apply { site = null }
        mockVariant.mockFlavors(flavorNames, fakeBuildTypeName)
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(flavorA)) doReturn variantConfigA
        whenever(fakeExtension.variants.findByName(flavorB)) doReturn variantConfigB
        whenever(fakeExtension.variants.findByName(flavorC)) doReturn variantConfigC

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfigB.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfigA.serviceName)
        assertThat(config.site).isEqualTo(variantConfigA.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfigC.checkProjectDependencies)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { complex variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigAB: DdExtensionConfiguration,
        @Forgery variantConfigAC: DdExtensionConfiguration,
        @Forgery variantConfigBC: DdExtensionConfiguration
    ) {
        val flavorNames = listOf(flavorA, flavorB, flavorC)
        variantConfigAB.apply { versionName = null }
        variantConfigAC.apply { serviceName = null }
        variantConfigBC.apply { site = null }
        variantConfigBC.apply { checkProjectDependencies = null }
        mockVariant.mockFlavors(flavorNames, fakeBuildTypeName)
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(flavorA + flavorB.capitalize()))
            .doReturn(variantConfigAB)
        whenever(fakeExtension.variants.findByName(flavorA + flavorC.capitalize()))
            .doReturn(variantConfigAC)
        whenever(fakeExtension.variants.findByName(flavorB + flavorC.capitalize()))
            .doReturn(variantConfigBC)

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfigAC.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfigAB.serviceName)
        assertThat(config.site).isEqualTo(variantConfigAB.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfigAB.checkProjectDependencies)
    }

    @Test
    fun `𝕄 return combined config 𝕎 resolveExtensionConfiguration() { variant w build type }`(
        @Forgery configuration: DdExtensionConfiguration
    ) {
        val variantName = fakeFlavorNames.variantName() + fakeBuildTypeName.capitalize()
        mockVariant.mockFlavors(fakeFlavorNames, fakeBuildTypeName)
        fakeExtension.variants = mock()
        whenever(fakeExtension.variants.findByName(variantName)) doReturn configuration

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(configuration.versionName)
        assertThat(config.serviceName).isEqualTo(configuration.serviceName)
        assertThat(config.site).isEqualTo(configuration.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(configuration.checkProjectDependencies)
    }

    // endregion

    // region configureVariantForSdkCheck

    @Test
    fun `𝕄 throw exception 𝕎 configureVariantForSdkCheck() { sdk is missing w fail set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = SdkCheckLevel.FAIL
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        val fakeCompileTask = fakeProject.task("compile${variantName.capitalize()}Sources")

        val mockConfiguration = mock<Configuration>()
        val mockResolvedConfiguration = mock<ResolvedConfiguration>()

        whenever(mockResolvedConfiguration.firstLevelModuleDependencies) doReturn emptySet()
        whenever(mockConfiguration.resolvedConfiguration) doReturn mockResolvedConfiguration
        whenever(mockVariant.runtimeConfiguration) doReturn mockConfiguration

        // When + Then
        val exception = assertThrows<MissingSdkException> {
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )!!.actions.first().execute(fakeCompileTask)
        }

        assertThat(exception.message)
            .isEqualTo(DdAndroidGradlePlugin.MISSING_DD_SDK_MESSAGE.format(variantName))
    }

    @Test
    fun `𝕄 throw exception 𝕎 configureVariantForSdkCheck() { sdk is missing w fail not set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = null
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        val fakeCompileTask = fakeProject.task("compile${variantName.capitalize()}Sources")

        val mockConfiguration = mock<Configuration>()
        val mockResolvedConfiguration = mock<ResolvedConfiguration>()

        whenever(mockResolvedConfiguration.firstLevelModuleDependencies) doReturn emptySet()
        whenever(mockConfiguration.resolvedConfiguration) doReturn mockResolvedConfiguration
        whenever(mockVariant.runtimeConfiguration) doReturn mockConfiguration

        // When + Then
        val exception = assertThrows<MissingSdkException> {
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )!!.actions.first().execute(fakeCompileTask)
        }

        assertThat(exception.message)
            .isEqualTo(DdAndroidGradlePlugin.MISSING_DD_SDK_MESSAGE.format(variantName))
    }

    @Test
    fun `𝕄 no exception thrown 𝕎 configureVariantForSdkCheck() { sdk is missing w warning set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = SdkCheckLevel.WARN
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        val fakeCompileTask = fakeProject.task("compile${variantName.capitalize()}Sources")

        val mockConfiguration = mock<Configuration>()
        val mockResolvedConfiguration = mock<ResolvedConfiguration>()

        whenever(mockResolvedConfiguration.firstLevelModuleDependencies) doReturn emptySet()
        whenever(mockConfiguration.resolvedConfiguration) doReturn mockResolvedConfiguration
        whenever(mockVariant.runtimeConfiguration) doReturn mockConfiguration

        // When + Then
        assertDoesNotThrow {
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )!!.actions.first().execute(fakeCompileTask)
        }
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariantForSdkCheck() { sdk is missing w none set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = SdkCheckLevel.NONE
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        val fakeCompileTask = fakeProject.task("compile${variantName.capitalize()}Sources")

        val mockConfiguration = mock<Configuration>()
        val mockResolvedConfiguration = mock<ResolvedConfiguration>()

        whenever(mockResolvedConfiguration.firstLevelModuleDependencies) doReturn emptySet()
        whenever(mockConfiguration.resolvedConfiguration) doReturn mockResolvedConfiguration
        whenever(mockVariant.runtimeConfiguration) doReturn mockConfiguration

        // When + Then
        assertDoesNotThrow {
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )!!.actions.first().execute(fakeCompileTask)
        }
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariantForSdkCheck() { sdk is there }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = SdkCheckLevel.FAIL
        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionName) doReturn versionName
        whenever(mockVariant.applicationId) doReturn packageName
        whenever(mockVariant.buildType) doReturn mockBuildType
        whenever(mockBuildType.name) doReturn fakeBuildTypeName

        val fakeCompileTask = fakeProject.task("compile${variantName.capitalize()}Sources")

        val mockConfiguration = mock<Configuration>()
        val mockResolvedConfiguration = mock<ResolvedConfiguration>()

        val mockResolvedDdSdkDependency = mock<ResolvedDependency>()
        whenever(mockResolvedDdSdkDependency.moduleName) doReturn "dd-sdk-android"
        whenever(mockResolvedDdSdkDependency.moduleGroup) doReturn "com.datadoghq"

        whenever(
            mockResolvedConfiguration.firstLevelModuleDependencies
        ) doReturn setOf(mockResolvedDdSdkDependency)

        whenever(mockConfiguration.resolvedConfiguration) doReturn mockResolvedConfiguration
        whenever(mockVariant.runtimeConfiguration) doReturn mockConfiguration

        // When + Then
        assertDoesNotThrow {
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )!!.actions.first().execute(fakeCompileTask)
        }
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariantForSdkCheck() { extension is disabled }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.enabled = false

        // When + Then
        assertThat(
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )
        ).isNull()
    }

    @Test
    fun `𝕄 do nothing 𝕎 configureVariantForSdkCheck() { compilation task not found }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @StringForgery packageName: String
    ) {

        // let's check that there are no tasks just in case if setup is modified
        assertThat(fakeProject.tasks.isEmpty()).isTrue()

        val variantName = "$flavorName${buildTypeName.capitalize()}"
        whenever(mockVariant.name) doReturn variantName

        assertThat(
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )
        ).isNull()
    }

    // endregion

    // region isDatadogDependencyPresent

    @Test
    fun `𝕄 return true 𝕎 isDatadogDependencyPresent() { sdk is at the top level }`(
        forge: Forge
    ) {

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

        assertThat(
            testedPlugin.isDatadogDependencyPresent(allDependencies)
        ).isTrue()
    }

    @Test
    fun `𝕄 return true 𝕎 isDatadogDependencyPresent() { sdk is at the child level }`(
        forge: Forge
    ) {

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

        assertThat(
            testedPlugin.isDatadogDependencyPresent(topDependencies.toSet())
        ).isTrue()
    }

    @Test
    fun `𝕄 return false 𝕎 isDatadogDependencyPresent() { sdk is not there }`(
        forge: Forge
    ) {
        val fakeDependencyGenerator: Forge.() -> ResolvedDependency = {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }

        val childDependencies = forge.aList(forging = fakeDependencyGenerator)
        val topDependencies = forge.aList(forging = fakeDependencyGenerator)

        whenever(topDependencies.random().children) doReturn childDependencies.toSet()

        assertThat(
            testedPlugin.isDatadogDependencyPresent(topDependencies.toSet())
        ).isFalse()
    }

    // endregion

    // region Internal

    private fun List<String>.variantName(): String {
        return first() + drop(1).joinToString("") { it.capitalize() }
    }

    private fun ApplicationVariant.mockFlavors(
        flavorNames: List<String>,
        buildTypeName: String
    ) {
        val mockFlavors: MutableList<ProductFlavor> = mutableListOf()
        for (flavorName in flavorNames) {
            mockFlavors.add(
                mock<ProductFlavor>().apply {
                    whenever(this.name) doReturn flavorName
                }
            )
        }
        val mockBuildType: BuildType = mock()
        whenever(mockBuildType.name) doReturn buildTypeName

        whenever(productFlavors) doReturn mockFlavors
        whenever(buildType) doReturn mockBuildType
    }

    // endregion
}
