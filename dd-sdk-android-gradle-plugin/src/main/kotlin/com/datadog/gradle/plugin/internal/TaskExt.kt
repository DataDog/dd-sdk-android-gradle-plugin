/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.android.build.gradle.tasks.ExternalNativeBuildTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal fun TaskProvider<ExternalNativeBuildTask>.getSearchObjDirs(): Provider<File> {
    return flatMap { task -> task.getSearchObjDirs() }
}

internal fun ExternalNativeBuildTask.getSearchObjDirs(): Provider<File> {
    return soFolder.map { it.asFile }
}
