/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.utils.capitalizeChar
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.annotation.StringForgery
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.artifacts.Configuration
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for DdAndroidGradlePlugin.configureVariantForSdkCheck() method.
 */
internal class DdAndroidGradlePluginConfigureVariantForSdkCheckTest : DdAndroidGradlePluginTestBase() {

    // TODO RUMM-2344 switch back to FAIL
    @Test
    fun `M use NONE when configuring checkDepsTask { checkProjectDependencies not set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String,
        @StringForgery configurationName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = null
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName

        fakeProject.task("compile${variantName.replaceFirstChar { capitalizeChar(it) }}Sources")

        val mockConfiguration = mock<Configuration>()
        whenever(mockConfiguration.name) doReturn configurationName

        whenever(mockVariant.compileConfiguration) doReturn mockConfiguration

        // When
        val checkSdkDepsTaskProvider = testedPlugin.configureVariantForSdkCheck(
            fakeProject,
            mockVariant,
            fakeExtension
        )

        // Then
        assertThat(checkSdkDepsTaskProvider).isNull()
    }

    @Test
    fun `M do nothing W configureVariantForSdkCheck() { none set }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String,
        @StringForgery versionName: String,
        @IntForgery(min = 1) versionCode: Int,
        @StringForgery packageName: String
    ) {
        // Given
        fakeExtension.checkProjectDependencies = SdkCheckLevel.NONE
        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName
        whenever(mockVariant.flavorName) doReturn flavorName
        whenever(mockVariant.versionCode) doReturn versionCode.asProvider()
        whenever(mockVariant.versionName) doReturn versionName.asProvider()
        whenever(mockVariant.applicationId) doReturn packageName.asProvider()
        whenever(mockVariant.buildTypeName) doReturn fakeBuildTypeName

        fakeProject.task("compile${variantName.replaceFirstChar { capitalizeChar(it) }}Sources")

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
    fun `M do nothing W configureVariantForSdkCheck() { extension is disabled }`() {
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
    fun `M do nothing W configureVariantForSdkCheck() { compilation task not found }`(
        @StringForgery(case = Case.LOWER) flavorName: String,
        @StringForgery(case = Case.LOWER) buildTypeName: String
    ) {
        // let's check that there are no tasks just in case if setup is modified
        assertThat(fakeProject.tasks.isEmpty()).isTrue()

        val variantName = "$flavorName${buildTypeName.replaceFirstChar { capitalizeChar(it) }}"
        whenever(mockVariant.name) doReturn variantName

        assertThat(
            testedPlugin.configureVariantForSdkCheck(
                fakeProject,
                mockVariant,
                fakeExtension
            )
        ).isNull()
    }
}
