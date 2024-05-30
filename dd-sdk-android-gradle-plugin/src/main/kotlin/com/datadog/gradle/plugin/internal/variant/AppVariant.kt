/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal.variant

import com.android.build.gradle.AppExtension
import com.datadog.gradle.plugin.GenerateBuildIdTask
import com.datadog.gradle.plugin.NdkSymbolFileUploadTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import com.android.build.api.variant.ApplicationVariant as NewApplicationVariant
import com.android.build.gradle.api.ApplicationVariant as LegacyApplicationVariant

internal interface AppVariant {

    val name: String

    val applicationId: Provider<String>

    val flavorName: String

    val versionCode: Provider<Int>

    val versionName: Provider<String>

    val compileConfiguration: Configuration

    val isNativeBuildEnabled: Boolean

    val isMinifyEnabled: Boolean

    val buildTypeName: String

    val flavors: List<String>

    val mappingFile: Provider<RegularFile>

    fun collectJavaAndKotlinSourceDirectories(): Provider<List<File>>

    fun bindWith(ndkUploadTask: NdkSymbolFileUploadTask)

    // new variant API doesn't allow to run addGeneratedSourceDirectory from inside Task#configure, thus this
    fun bindWith(generateBuildIdTask: TaskProvider<GenerateBuildIdTask>, buildIdDirectory: Provider<Directory>)

    companion object {
        fun create(
            variant: NewApplicationVariant,
            target: Project
        ): AppVariant = NewApiAppVariant(variant, target)

        fun create(
            variant: LegacyApplicationVariant,
            appExtension: AppExtension,
            target: Project
        ): AppVariant = LegacyApiAppVariant(variant, appExtension, target)
    }
}
