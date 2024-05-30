/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.DatadogSite
import com.datadog.gradle.plugin.DdAndroidGradlePlugin.Companion.LOGGER
import com.datadog.gradle.plugin.RepositoryInfo
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import okio.use
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.util.Locale
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
        site: DatadogSite,
        fileInfo: Uploader.UploadFileInfo,
        repositoryFile: File?,
        apiKey: String,
        identifier: DdAppIdentifier,
        repositoryInfo: RepositoryInfo?,
        useGzip: Boolean,
        emulateNetworkCall: Boolean
    ) {
        LOGGER.info("Uploading file ${fileInfo.fileName} with tags $identifier (site=${site.domain}):")
        if (fileInfo.extraAttributes.isNotEmpty()) {
            LOGGER.info("  extra attributes: ${fileInfo.extraAttributes}")
        }
        LOGGER.info("\n")
        val body = createBody(identifier, fileInfo, repositoryFile, repositoryInfo)

        val requestBuilder = Request.Builder()
            .url(site.uploadEndpoint())
            .header(HEADER_EVP_ORIGIN, "dd-sdk-android-gradle-plugin")
            .header(HEADER_EVP_ORIGIN_VERSION, VERSION)
            .header(HEADER_API_KEY, apiKey)
        val request = if (useGzip) {
            LOGGER.info("Creating request with GZIP encoding.")
            requestBuilder
                .post(body.gzip())
                .header(HEADER_CONTENT_ENCODING, ENCODING_GZIP)
                .build()
        } else {
            LOGGER.info("Creating request without GZIP encoding.")
            requestBuilder
                .post(body)
                .build()
        }

        val call = client.newCall(request)
        val response = try {
            if (emulateNetworkCall) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_2)
                    .message("fake-response")
                    .code(HttpURLConnection.HTTP_ACCEPTED)
                    .build()
            } else {
                call.execute()
            }
        } catch (e: Throwable) {
            LOGGER.error("Error uploading the mapping file for $identifier", e)
            null
        }

        handleResponse(response, site, apiKey, identifier)
    }

    // endregion

    // region Internal

    private fun createBody(
        identifier: DdAppIdentifier,
        fileInfo: Uploader.UploadFileInfo,
        repositoryFile: File?,
        repositoryInfo: RepositoryInfo?
    ): MultipartBody {
        val mappingFileBody = fileInfo.file.asRequestBody(fileInfo.encoding.toMediaTypeOrNull())

        val eventJson = JSONObject()
        eventJson.put("version", identifier.version)
        eventJson.put("service", identifier.serviceName)
        eventJson.put("variant", identifier.variant)
        eventJson.put("build_id", identifier.buildId)
        eventJson.put("version_code", identifier.versionCode)
        eventJson.put("type", fileInfo.fileType)
        fileInfo.extraAttributes.forEach { (key, value) ->
            eventJson.put(key, value)
        }
        LOGGER.info("  event: ${eventJson.toString(0)}")

        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
            .addFormDataPart(
                KEY_EVENT,
                KEY_EVENT,
                eventJson.toString(0).toRequestBody(MEDIA_TYPE_JSON)
            )
            .addFormDataPart(
                fileInfo.fileKey,
                fileInfo.fileName,
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

    @Suppress("ThrowingInternalException", "TooGenericExceptionThrown", "ThrowsCount")
    private fun handleResponse(
        response: Response?,
        site: DatadogSite,
        apiKey: String,
        identifier: DdAppIdentifier
    ) {
        val statusCode = response?.code
        when {
            statusCode == null -> throw RuntimeException(
                "Unable to upload mapping file with tags $identifier; check your network connection"
            )
            statusCode in successfulCodes -> LOGGER.quiet(
                "Mapping file upload successful with tags $identifier. \n" +
                    "Make sure the SDK is initialized with the same values to ensure " +
                    "proper deobfuscation. Mapping files will be processed and ready within " +
                    "the next five minutes."
            )
            statusCode == HttpURLConnection.HTTP_FORBIDDEN -> throw InvalidApiKeyException(
                identifier,
                site
            )
            statusCode == HttpURLConnection.HTTP_CLIENT_TIMEOUT -> throw RuntimeException(
                "Unable to upload mapping file with tags $identifier because of a request timeout; " +
                    "check your network connection"
            )
            statusCode == HttpURLConnection.HTTP_ENTITY_TOO_LARGE ->
                throw MaxSizeExceededException(
                    MAX_MAP_SIZE_EXCEEDED_ERROR.format(Locale.US, identifier)
                )
            statusCode >= HttpURLConnection.HTTP_BAD_REQUEST -> {
                if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST &&
                    validateApiKey(site, apiKey) == false
                ) {
                    throw InvalidApiKeyException(identifier, site)
                }
                response.body.use {
                    throw IllegalStateException(
                        "Unable to upload mapping file with tags $identifier ($statusCode);\n" +
                            "${it?.string()}"
                    )
                }
            }
        }
    }

    private fun validateApiKey(site: DatadogSite, apiKey: String): Boolean? {
        val request = Request.Builder()
            .url(site.apiKeyVerificationEndpoint())
            .header(HEADER_API_KEY, apiKey)
            .build()

        val call = client.newCall(request)
        val response = try {
            call.execute()
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            LOGGER.error("Error checking the validity of API key", e)
            null
        } ?: return null

        return when (response.code) {
            HttpURLConnection.HTTP_OK -> {
                val body = response.body?.string()
                if (body != null) {
                    try {
                        val json = JSONObject(JSONTokener(body))
                        json.getBoolean("valid")
                    } catch (jse: JSONException) {
                        LOGGER.error("Unexpected format of API key validity check response", jse)
                        null
                    }
                } else {
                    null
                }
            }
            HttpURLConnection.HTTP_FORBIDDEN -> false
            else -> null
        }
    }

    internal inner class InvalidApiKeyException(
        uploadIdentifier: DdAppIdentifier,
        site: DatadogSite
    ) : RuntimeException(
        "Unable to upload mapping file for $uploadIdentifier (site=${site.domain}); " +
            "verify that you're using a valid API Key"
    )

    // endregion

    private fun RequestBody.gzip(): RequestBody {
        val uncompressedBody = this
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return uncompressedBody.contentType()
            }

            override fun contentLength(): Long {
                return -1 // We don't know the compressed length in advance!
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val gzipSink = GzipSink(sink).buffer()
                uncompressedBody.writeTo(gzipSink)
                gzipSink.close()
            }

            override fun isOneShot(): Boolean {
                return uncompressedBody.isOneShot()
            }
        }
    }

    companion object {

        // TODO add a plugin to automatically sync this with the `MavenConfig` value
        internal const val VERSION = "1.13.0"

        internal const val HEADER_API_KEY = "DD-API-KEY"
        internal const val HEADER_EVP_ORIGIN = "DD-EVP-ORIGIN"
        internal const val HEADER_EVP_ORIGIN_VERSION = "DD-EVP-ORIGIN-VERSION"
        internal const val HEADER_REQUEST_ID = "DD-REQUEST-ID"
        internal const val HEADER_CONTENT_ENCODING = "Content-Encoding"
        internal const val ENCODING_GZIP = "gzip"

        internal const val KEY_EVENT = "event"
        internal const val KEY_REPOSITORY = "repository"

        internal val NETWORK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(120)
        internal val MEDIA_TYPE_JSON = "application/json".toMediaTypeOrNull()

        internal const val MAX_MAP_SIZE_EXCEEDED_ERROR =
            "Unable to upload mapping file for %s because mapping file is too" +
                " large; please refer to documentation regarding the limits"

        internal val successfulCodes = arrayOf(
            HttpURLConnection.HTTP_OK,
            HttpURLConnection.HTTP_CREATED,
            HttpURLConnection.HTTP_ACCEPTED
        )
    }
}
