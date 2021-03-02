/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.RepositoryInfo
import java.io.File
import java.lang.IllegalStateException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class OkHttpUploader : Uploader {

    // region Uploader

    internal val client = OkHttpClient
        .Builder()
        .callTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    @Suppress("TooGenericExceptionCaught")
    override fun upload(
        url: String,
        mappingFile: File,
        repositoryFile: File?,
        identifier: DdAppIdentifier,
        repositoryInfo: RepositoryInfo?
    ) {
        LOGGER.info("Uploading mapping file for $identifier:\n")
        val body = createBody(identifier, mappingFile, repositoryFile, repositoryInfo)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val call = client.newCall(request)
        val response = try {
            call.execute()
        } catch (e: Throwable) {
            LOGGER.error("Error uploading the mapping file for $identifier", e)
            null
        }

        handleResponse(response, identifier)
    }

    // endregion

    // region Internal

    private fun createBody(
        identifier: DdAppIdentifier,
        mappingFile: File,
        repositoryFile: File?,
        repositoryInfo: RepositoryInfo?
    ): MultipartBody {
        val mappingFileBody = MultipartBody.create(MEDIA_TYPE_TXT, mappingFile)

        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
            .addFormDataPart("version", identifier.version)
            .addFormDataPart("service", identifier.serviceName)
            .addFormDataPart("variant", identifier.variant)
            .addFormDataPart("type", TYPE_JVM_MAPPING_FILE)
            .addFormDataPart("jvm_mapping_file", mappingFile.name, mappingFileBody)

        if (repositoryFile != null) {
            val repositoryFileBody = MultipartBody.create(MEDIA_TYPE_JSON, repositoryFile)
            builder.addFormDataPart("repository", repositoryFile.name, repositoryFileBody)
        }
        if (repositoryInfo != null) {
            builder.addFormDataPart("git_repository_url", repositoryInfo.url)
            builder.addFormDataPart("git_commit_sha", repositoryInfo.hash)
        }

        return builder.build()
    }

    @Suppress("ThrowingInternalException", "TooGenericExceptionThrown")
    private fun handleResponse(
        response: Response?,
        identifier: DdAppIdentifier
    ) {
        val statusCode = response?.code()
        when {
            statusCode == null -> throw RuntimeException(
                "Unable to upload mapping file for $identifier; check your network connection"
            )
            statusCode in succesfulCodes -> LOGGER.info(
                "Mapping file upload successful for $identifier"
            )
            statusCode == HttpURLConnection.HTTP_FORBIDDEN -> throw IllegalStateException(
                "Unable to upload mapping file for $identifier; " +
                    "verify that you're using a valid API Key"
            )
            statusCode >= HttpURLConnection.HTTP_BAD_REQUEST -> throw IllegalStateException(
                "Unable to upload mapping file for $identifier; " +
                    "it can be because the mapping file already exist for this version"
            )
        }
    }

    // endregion

    companion object {

        internal val NETWORK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(45)
        internal val MEDIA_TYPE_TXT = MediaType.parse("text/plain")
        internal val MEDIA_TYPE_JSON = MediaType.parse("application/json")

        internal val succesfulCodes = arrayOf(
            HttpURLConnection.HTTP_OK,
            HttpURLConnection.HTTP_CREATED,
            HttpURLConnection.HTTP_ACCEPTED
        )

        internal const val TYPE_JVM_MAPPING_FILE = "jvm_mapping_file"
    }
}
