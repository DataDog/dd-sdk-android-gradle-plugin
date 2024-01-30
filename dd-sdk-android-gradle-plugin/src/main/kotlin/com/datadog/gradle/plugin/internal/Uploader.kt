/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DatadogSite
import com.datadog.gradle.plugin.RepositoryInfo
import java.io.File

internal interface Uploader {
    // region File Info

    data class UploadFileInfo(
        val fileKey: String,
        val file: File,
        val encoding: String,
        val fileType: String,
        val fileName: String,
        val extraAttributes: Map<String, String> = emptyMap()
    )

    // endregion

    @Suppress("LongParameterList")
    fun upload(
        site: DatadogSite,
        fileInfo: UploadFileInfo,
        repositoryFile: File?,
        apiKey: String,
        identifier: DdAppIdentifier,
        repositoryInfo: RepositoryInfo?,
        useGzip: Boolean
    )
}
