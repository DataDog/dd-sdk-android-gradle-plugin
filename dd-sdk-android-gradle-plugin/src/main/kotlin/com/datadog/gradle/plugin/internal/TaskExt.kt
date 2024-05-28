/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.android.build.gradle.tasks.ExternalNativeBuildTask
import com.datadog.gradle.plugin.GenerateBuildIdTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import java.io.File
import kotlin.reflect.full.memberProperties

internal fun TaskProvider<ExternalNativeBuildTask>.getSearchObjDirs(providerFactory: ProviderFactory): Provider<File?> {
    return flatMap { task -> task.getSearchObjDirs(providerFactory) }
}

internal fun ExternalNativeBuildTask.getSearchObjDirs(providerFactory: ProviderFactory): Provider<File?> {
    return if (CurrentAgpVersion.EXTERNAL_NATIVE_BUILD_SOFOLDER_IS_PUBLIC) {
        soFolder.map { it.asFile }
    } else {
        val soFolder = ExternalNativeBuildTask::class.memberProperties.find {
            it.name == "objFolder"
        }?.get(this)
        when (soFolder) {
            is File -> providerFactory.provider { soFolder }
            is DirectoryProperty -> soFolder.map { it.asFile }
            else -> providerFactory.provider { null }
        }
    }
}

internal fun TaskProvider<GenerateBuildIdTask>.lazyBuildIdProvider(providerFactory: ProviderFactory): Provider<String> {
    // upload task shouldn't depend on the build ID generation task, but only read its property,
    // because upload task may be triggered after assemble task and we don't want to re-generate
    // build ID, because it will be different then from the one which is already embedded in
    // the application package
    return flatMap {
        it.buildIdFile.flatMap {
            providerFactory.provider {
                val file = it.asFile
                if (file.exists()) {
                    file.readText().trim()
                } else {
                    ""
                }
            }
        }
    }
}
