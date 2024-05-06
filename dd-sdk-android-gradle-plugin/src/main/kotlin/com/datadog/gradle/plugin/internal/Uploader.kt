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
        /**
         * The form key to use when uploading the file.
         */
        val fileKey: String,

        /**
         * The file to upload.
         */
        val file: File,

        /**
         * The encoding to use during upload of the file.
         */
        val encoding: String,

        /**
         * The type of file given to intake, e.g. `jvm_mapping` or `ndk_symbol_file`.
         */
        val fileType: String,

        /**
         * The name of the file to provide to intake. This can be different from the name
         * of the file on disk.
         */
        val fileName: String,

        /**
         * Any extra attributes to provide to intake, such as the architecture of an NDK symbol file.
         */
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
        useGzip: Boolean = true,
        emulateNetworkCall: Boolean = false
    )
}
