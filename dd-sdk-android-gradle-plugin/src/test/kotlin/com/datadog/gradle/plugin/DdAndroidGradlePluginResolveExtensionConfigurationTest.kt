/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.utils.capitalizeChar
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.annotation.AdvancedForgery
import fr.xgouchet.elmyr.annotation.BoolForgery
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.MapForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

internal class DdAndroidGradlePluginResolveExtensionConfigurationTest : DdAndroidGradlePluginTestBase() {

    @Test
    fun `M return default config W resolveExtensionConfiguration() {no variant config}`() {
        // Given
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(fakeExtension.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(fakeExtension.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(fakeExtension.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return config W resolveExtensionConfiguration() { variant w full config }`(
        @Forgery variantConfig: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(fakeExtension.variants.findByName(variantName)) doReturn variantConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfig.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfig.serviceName)
        assertThat(config.site).isEqualTo(variantConfig.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(variantConfig.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfig.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(variantConfig.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(variantConfig.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(variantConfig.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(variantConfig.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w version only }`(
        @StringForgery versionName: String
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.versionName = versionName
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w service only }`(
        @StringForgery serviceName: String
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.serviceName = serviceName
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w mappingPath }`(
        @StringForgery(regex = "/([a-z]+)/([a-z]+)/([a-z]+)/mapping.txt") mappingFilePath: String
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.mappingFilePath = mappingFilePath
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.mappingFilePath).isEqualTo(mappingFilePath)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return config W resolveExtensionConfiguration() { variant+mappingFilePackageAliases }`(
        @MapForgery(
            key = AdvancedForgery(
                string = [StringForgery(regex = "[a-z]{3}(\\.[a-z]{5,10}){2,4}")]
            ),
            value = AdvancedForgery(string = [StringForgery(StringForgeryType.ALPHABETICAL)])
        ) mappingFilePackageAliases: Map<String, String>
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.mappingFilePackageAliases = mappingFilePackageAliases
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return config W resolveExtensionConfiguration() { variant+mappingFileTrimIndents }`(
        @BoolForgery mappingFileTrimIndents: Boolean
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.mappingFileTrimIndents = mappingFileTrimIndents
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(fakeExtension.checkProjectDependencies)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEmpty()
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w site only }`(
        @Forgery site: DatadogSite
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.site = site.name
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(site.name)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(
            fakeExtension.checkProjectDependencies
        )
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w sdkCheck only }`(
        @Forgery sdkCheckLevel: SdkCheckLevel
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.checkProjectDependencies = sdkCheckLevel
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(sdkCheckLevel)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w remoteUrl only }`(
        @Forgery fakeConfig: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.remoteRepositoryUrl = fakeConfig.remoteRepositoryUrl
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(incompleteConfig.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(
            fakeExtension.checkProjectDependencies
        )
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig).isFalse
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant + ignoreDdConfig }`(
        @Forgery fakeConfig: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        val incompleteConfig = DdExtensionConfiguration().apply {
            this.ignoreDatadogCiFileConfig = fakeConfig.ignoreDatadogCiFileConfig
        }
        whenever(fakeExtension.variants.findByName(variantName)) doReturn incompleteConfig

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(fakeExtension.versionName)
        assertThat(config.serviceName).isEqualTo(fakeExtension.serviceName)
        assertThat(config.site).isEqualTo(fakeExtension.site)
        assertThat(config.remoteRepositoryUrl).isEqualTo(fakeExtension.remoteRepositoryUrl)
        assertThat(config.checkProjectDependencies).isEqualTo(
            fakeExtension.checkProjectDependencies
        )
        assertThat(config.mappingFilePath).isEqualTo(fakeExtension.mappingFilePath)
        assertThat(config.mappingFilePackageAliases).isEmpty()
        assertThat(config.mappingFileTrimIndents).isFalse
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(incompleteConfig.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { simple variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigA: DdExtensionConfiguration,
        @Forgery variantConfigB: DdExtensionConfiguration,
        @Forgery variantConfigC: DdExtensionConfiguration
    ) {
        // Given
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
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn flavorNames
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
        assertThat(config.remoteRepositoryUrl).isEqualTo(variantConfigA.remoteRepositoryUrl)
        assertThat(config.mappingFilePath).isEqualTo(variantConfigA.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(variantConfigA.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(variantConfigA.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(variantConfigA.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { complex variants }`(
        @StringForgery(case = Case.LOWER) flavorA: String,
        @StringForgery(case = Case.LOWER) flavorB: String,
        @StringForgery(case = Case.LOWER) flavorC: String,
        @Forgery variantConfigAB: DdExtensionConfiguration,
        @Forgery variantConfigAC: DdExtensionConfiguration,
        @Forgery variantConfigBC: DdExtensionConfiguration
    ) {
        // Given
        val flavorNames = listOf(flavorA, flavorB, flavorC)
        variantConfigAB.apply { versionName = null }
        variantConfigAC.apply { serviceName = null }
        variantConfigBC.apply { site = null }
        variantConfigBC.apply { checkProjectDependencies = null }
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn flavorNames
        whenever(
            fakeExtension.variants.findByName(
                flavorA + flavorB.replaceFirstChar { capitalizeChar(it) }
            )
        )
            .doReturn(variantConfigAB)
        whenever(
            fakeExtension.variants.findByName(
                flavorA + flavorC.replaceFirstChar { capitalizeChar(it) }
            )
        )
            .doReturn(variantConfigAC)
        whenever(
            fakeExtension.variants.findByName(
                flavorB + flavorC.replaceFirstChar { capitalizeChar(it) }
            )
        )
            .doReturn(variantConfigBC)

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(variantConfigAC.versionName)
        assertThat(config.serviceName).isEqualTo(variantConfigAB.serviceName)
        assertThat(config.site).isEqualTo(variantConfigAB.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(variantConfigAB.checkProjectDependencies)
        assertThat(config.remoteRepositoryUrl).isEqualTo(variantConfigAB.remoteRepositoryUrl)
        assertThat(config.mappingFilePath).isEqualTo(variantConfigAB.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(variantConfigAB.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(variantConfigAB.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(variantConfigAB.ignoreDatadogCiFileConfig)
    }

    @Test
    fun `M return combined config W resolveExtensionConfiguration() { variant w build type }`(
        @Forgery configuration: DdExtensionConfiguration
    ) {
        // Given
        val variantName = fakeFlavorNames.variantName() +
            fakeBuildTypeName.replaceFirstChar { capitalizeChar(it) }
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName
        whenever(mockVariant.flavors) doReturn fakeFlavorNames
        whenever(fakeExtension.variants.findByName(variantName)) doReturn configuration

        // When
        val config = testedPlugin.resolveExtensionConfiguration(fakeExtension, mockVariant)

        // Then
        assertThat(config.versionName).isEqualTo(configuration.versionName)
        assertThat(config.serviceName).isEqualTo(configuration.serviceName)
        assertThat(config.site).isEqualTo(configuration.site)
        assertThat(config.checkProjectDependencies)
            .isEqualTo(configuration.checkProjectDependencies)
        assertThat(config.remoteRepositoryUrl).isEqualTo(configuration.remoteRepositoryUrl)
        assertThat(config.mappingFilePath).isEqualTo(configuration.mappingFilePath)
        assertThat(config.mappingFilePackageAliases)
            .isEqualTo(configuration.mappingFilePackageAliases)
        assertThat(config.mappingFileTrimIndents)
            .isEqualTo(configuration.mappingFileTrimIndents)
        assertThat(config.ignoreDatadogCiFileConfig)
            .isEqualTo(configuration.ignoreDatadogCiFileConfig)
    }
}
