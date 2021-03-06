/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.RepositoryInfo
import java.io.File

internal interface Uploader {

    fun upload(
        url: String,
        mappingFile: File,
        repositoryFile: File?,
        identifier: DdAppIdentifier,
        repositoryInfo: RepositoryInfo?
    )
}
