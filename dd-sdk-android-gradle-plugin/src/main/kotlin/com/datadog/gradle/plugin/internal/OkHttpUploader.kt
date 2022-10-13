/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.RepositoryInfo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

internal class OkHttpUploader : Uploader {

    // region Uploader

    // we cannot make it a property with a backing field, because serialization of this field is not
    // supported (serialization is used by configuration cache)
    internal val client
        get() = OkHttpClient
            .Builder()
            .callTimeout(0, TimeUnit.MILLISECONDS) // unlimited
            .writeTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()

    @Suppress("TooGenericExceptionCaught")
    override fun upload(
        url: String,
        mappingFile: File,
        repositoryFile: File?,
        apiKey: String,
        identifier: DdAppIdentifier,
        repositoryInfo: RepositoryInfo?
    ) {
        LOGGER.info("Uploading mapping file for $identifier:\n")
        val body = createBody(identifier, mappingFile, repositoryFile, repositoryInfo)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header(HEADER_EVP_ORIGIN, "dd-sdk-android-gradle-plugin")
            .header(HEADER_EVP_ORIGIN_VERSION, VERSION)
            .header(HEADER_API_KEY, apiKey)
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

        val mappingFileBody = mappingFile.asRequestBody(MEDIA_TYPE_TXT)
        if (mappingFileBody.contentLength() > MAX_MAP_FILE_SIZE_IN_BYTES) {
            throw MaxSizeExceededException(
                MAX_MAP_SIZE_EXCEEDED_ERROR_FORMAT
                    .format(mappingFile.absolutePath)
            )
        }

        val eventJson = JSONObject()
        eventJson.put("version", identifier.version)
        eventJson.put("service", identifier.serviceName)
        eventJson.put("variant", identifier.variant)
        eventJson.put("type", TYPE_JVM_MAPPING_FILE)

        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
            .addFormDataPart(
                KEY_EVENT,
                KEY_EVENT,
                eventJson.toString(0).toRequestBody(MEDIA_TYPE_JSON)
            )
            .addFormDataPart(
                KEY_JVM_MAPPING_FILE,
                KEY_JVM_MAPPING,
                mappingFileBody
            )

        if (repositoryFile != null) {
            val repositoryFileBody = repositoryFile.asRequestBody(MEDIA_TYPE_JSON)
            builder.addFormDataPart(
                KEY_REPOSITORY,
                KEY_REPOSITORY,
                repositoryFileBody
            )
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
        val statusCode = response?.code
        when {
            statusCode == null -> throw RuntimeException(
                "Unable to upload mapping file for $identifier; check your network connection"
            )
            statusCode in successfulCodes -> LOGGER.info(
                "Mapping file upload successful for $identifier"
            )
            statusCode == HttpURLConnection.HTTP_FORBIDDEN -> throw IllegalStateException(
                "Unable to upload mapping file for $identifier; " +
                    "verify that you're using a valid API Key"
            )
            statusCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT -> throw RuntimeException(
                "Unable to upload mapping file for $identifier because of a request timeout; " +
                    "check your network connection"
            )
            statusCode >= HttpURLConnection.HTTP_BAD_REQUEST -> {
                throw IllegalStateException(
                    "Unable to upload mapping file for $identifier ($statusCode); " +
                        "it can be because the mapping file already exist for this version.\n" +
                        "${response.body?.string()}"
                )
            }
        }
    }

    // endregion

    companion object {

        // TODO add a plugin to automatically sync this with the `MavenConfig` value
        internal const val VERSION = "1.1.0"

        internal const val HEADER_API_KEY = "DD-API-KEY"
        internal const val HEADER_EVP_ORIGIN = "DD-EVP-ORIGIN"
        internal const val HEADER_EVP_ORIGIN_VERSION = "DD-EVP-ORIGIN-VERSION"
        internal const val HEADER_REQUEST_ID = "DD-REQUEST-ID"

        internal const val KEY_EVENT = "event"
        internal const val KEY_JVM_MAPPING_FILE = "jvm_mapping_file"
        internal const val KEY_JVM_MAPPING = "jvm_mapping"
        internal const val KEY_REPOSITORY = "repository"

        internal val NETWORK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(45)
        internal val MEDIA_TYPE_TXT = "text/plain".toMediaTypeOrNull()
        internal val MEDIA_TYPE_JSON = "application/json".toMediaTypeOrNull()
        internal const val MAX_MAP_FILE_SIZE_IN_BYTES = 50L * 1024L * 1024L // 50 MB
        internal const val MAX_MAP_SIZE_EXCEEDED_ERROR_FORMAT =
            "The proguard mapping file at: [%s] size exceeded the maximum 50 MB size. " +
                "This task cannot be performed."
        internal val successfulCodes = arrayOf(
            HttpURLConnection.HTTP_OK,
            HttpURLConnection.HTTP_CREATED,
            HttpURLConnection.HTTP_ACCEPTED
        )

        internal const val TYPE_JVM_MAPPING_FILE = "jvm_mapping_file"
    }
}
