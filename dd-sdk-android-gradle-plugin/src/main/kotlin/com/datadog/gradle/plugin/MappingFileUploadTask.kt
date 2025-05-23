/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.Uploader
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import java.io.File
import javax.inject.Inject

/**
 * A Gradle task to upload a Proguard/R8 mapping file to Datadog servers.
 */
abstract class MappingFileUploadTask
@Inject constructor(
    providerFactory: ProviderFactory,
    repositoryDetector: RepositoryDetector
) : FileUploadTask(providerFactory, repositoryDetector) {

    /**
     * The path to the mapping file to upload.
     */
    // don't use InputFile here, because it will check if file exists before executing task,
    // we want to do it manually and show our own error
    @get:InputFiles
    abstract val mappingFile: RegularFileProperty

    /**
     * Replacements for the source prefixes in the mapping file.
     */
    @get:Input
    var mappingFilePackagesAliases: Map<String, String> = emptyMap()

    /**
     * Trim indents in the mapping file.
     */
    @get:Input
    var mappingFileTrimIndents: Boolean = false

    /**
     * Application ID (usually a package prefix).
     */
    @get:Input
    abstract val applicationId: Property<String>

    init {
        group = DdAndroidGradlePlugin.DATADOG_TASK_GROUP
        description = "Uploads the Proguard/R8 mapping file to Datadog"
        // it is never up-to-date, because request may fail
        outputs.upToDateWhen { false }
    }

    override fun getFilesList(): List<Uploader.UploadFileInfo> {
        check(mappingFile.isPresent) {
            "No mapping file value in the input property."
        }
        var mappingFile = mappingFile.get().asFile
        if (!validateMappingFile(mappingFile)) return emptyList()

        mappingFile = shrinkMappingFile(mappingFile)

        return listOf(
            Uploader.UploadFileInfo(
                fileKey = KEY_JVM_MAPPING_FILE,
                file = mappingFile,
                encoding = MEDIA_TYPE_TXT,
                fileType = TYPE_JVM_MAPPING_FILE,
                fileName = KEY_JVM_MAPPING_FILE_NAME
            )
        )
    }

    @Suppress("CheckInternal")
    private fun validateMappingFile(mappingFile: File): Boolean {
        if (!mappingFile.exists()) {
            println("There's no mapping file ${mappingFile.absolutePath}, nothing to upload")
            return false
        }

        check(mappingFile.isFile) { "Expected ${mappingFile.absolutePath} to be a file" }

        check(mappingFile.canRead()) { "Cannot read file ${mappingFile.absolutePath}" }

        return true
    }

    private fun shrinkMappingFile(
        mappingFile: File
    ): File {
        if (!mappingFileTrimIndents && mappingFilePackagesAliases.isEmpty()) return mappingFile

        LOGGER.info(
            "Size of ${mappingFile.path} before optimization" +
                " is ${mappingFile.length()} bytes"
        )

        val replacements = mappingFilePackagesAliases
            .filter {
                // not necessarily applicationId == package attribute from the Manifest, but it is
                // best and cheapest effort to avoid wrong renaming (otherwise we may loose Git
                // integration feature).
                if (applicationId.get().startsWith(it.key)) {
                    DdAndroidGradlePlugin.LOGGER.warn(
                        "Renaming of package prefix=${it.key} will be skipped, because" +
                            " it belongs to the application package."
                    )
                    false
                } else {
                    true
                }
            }
            .entries
            // sort is needed to have predictable replacement in the following case:
            // imagine there are 2 keys - "androidx.work" and "androidx.work.Job", and the latter
            // occurs much more often than the rest under "androidx.work.*". So for the more efficient
            // compression we need first to process the replacement of "androidx.work.Job" and only
            // after that any possible prefix (which has a smaller length).
            .sortedByDescending { it.key.length }
            .map {
                val lookup = it.key.replace(".*", "")
                    .replace(".", "\\.")
                Regex("(?<=^|\\W)$lookup(?=\\W)") to it.value
            }

        val shrinkedFile = File(mappingFile.parent, MAPPING_OPTIMIZED_FILE_NAME)
        if (shrinkedFile.exists()) {
            shrinkedFile.delete()
        }

        shrinkedFile.bufferedWriter(bufferSize = MAPPING_OPTIMIZED_FILE_WRITE_BUFFER_SIZE)
            .use { shrinkedFileWriter ->
                mappingFile.useLines { lines ->
                    lines.run { if (mappingFileTrimIndents) map { it.trimStart() } else this }
                        .map {
                            applyShortAliases(it, replacements)
                        }
                        .forEach {
                            shrinkedFileWriter.appendLine(it)
                        }
                    shrinkedFileWriter.flush()
                }
            }

        LOGGER.info("Size of optimized file is ${shrinkedFile.length()} bytes")

        return shrinkedFile
    }

    private fun applyShortAliases(
        line: String,
        replacements: List<Pair<Regex, String>>
    ): String {
        val isLineWithRename = !line.startsWith(MAPPING_FILE_COMMENT_CHAR) &&
            line.contains(MAPPING_FILE_CHANGE_DELIMITER)

        return if (isLineWithRename) {
            val parts = line.split(MAPPING_FILE_CHANGE_DELIMITER)
            if (parts.size != 2) {
                LOGGER.info(
                    "Unexpected number of '$MAPPING_FILE_CHANGE_DELIMITER'" +
                        " (${parts.size - 1}) in the obfuscation line."
                )
                line
            } else {
                val (source, obfuscated) = parts
                val aliasedSource =
                    replacements.fold(source) { state, entry ->
                        val (from, to) = entry
                        state.replace(from, to)
                    }
                "$aliasedSource$MAPPING_FILE_CHANGE_DELIMITER$obfuscated"
            }
        } else {
            line
        }
    }

    // endregion

    internal companion object {
        internal const val TYPE_JVM_MAPPING_FILE = "jvm_mapping_file"
        internal const val KEY_JVM_MAPPING_FILE = "jvm_mapping_file"
        internal const val KEY_JVM_MAPPING_FILE_NAME = "jvm_mapping"
        internal const val MEDIA_TYPE_TXT = "text/plain"

        private const val MAPPING_FILE_CHANGE_DELIMITER = "->"
        private const val MAPPING_FILE_COMMENT_CHAR = '#'
        const val MAPPING_OPTIMIZED_FILE_NAME = "mapping-shrinked.txt"
        const val MAPPING_OPTIMIZED_FILE_WRITE_BUFFER_SIZE = 32 * 1024 // 32 Kb

        const val API_KEY_MISSING_ERROR = "Make sure you define an API KEY to upload your mapping files to Datadog. " +
            "Create a DD_API_KEY or DATADOG_API_KEY environment variable, gradle" +
            " property or define it in datadog-ci.json file."
        const val INVALID_API_KEY_FORMAT_ERROR =
            "DD_API_KEY provided shouldn't contain quotes or apostrophes."
        const val MISSING_BUILD_ID_ERROR =
            "Build ID is missing, you need to run upload task only after APK/AAB file is generated."
    }
}
