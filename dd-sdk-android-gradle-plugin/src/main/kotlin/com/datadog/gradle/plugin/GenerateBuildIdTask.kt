package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.util.UUID

/**
 * This task generates unique Build ID which is later used to match error and mapping file.
 */
abstract class GenerateBuildIdTask : DefaultTask() {

    /**
     * Directory to store build ID file.
     */
    @get:OutputDirectory
    abstract val buildIdDirectory: DirectoryProperty

    /**
     * File containing build ID.
     */
    @get:Internal
    val buildIdFile: Provider<RegularFile> = buildIdDirectory.file(BUILD_ID_FILE_NAME)

    /**
     * Variant name this task is linked to.
     */
    @get:Internal
    abstract val variantName: Property<String>

    init {
        outputs.upToDateWhen { false }
        // not a part of any group, we don't want to expose it
        description = "Generates a unique build ID to associate mapping file and application."
    }

    /**
     * Generates unique build ID and saves it to a file.
     */
    @TaskAction
    fun generateBuildId() {
        val buildIdDirectory = buildIdDirectory.get().asFile
        buildIdDirectory.mkdirs()

        val buildId = UUID.randomUUID().toString()
        logger.info("Generated buildId=$buildId for variant=${variantName.get()}")
        buildIdFile.get().asFile
            .writeText(buildId)
    }

    companion object {
        internal const val TASK_NAME = "generateBuildId"

        /**
         * Name of the file containing build ID information.
         */
        const val BUILD_ID_FILE_NAME = "datadog.buildId"

        /**
         * Registers a new instance of [GenerateBuildIdTask] specific for the given [ApplicationVariant].
         */
        fun register(
            target: Project,
            variant: ApplicationVariant,
            buildIdDirectory: Provider<Directory>
        ): TaskProvider<GenerateBuildIdTask> {
            val generateBuildIdTask = target.tasks.register(
                TASK_NAME + variant.name.capitalize(),
                GenerateBuildIdTask::class.java
            ) {
                it.buildIdDirectory.set(buildIdDirectory)
                it.variantName.set(variant.name)
            }

            return generateBuildIdTask
        }
    }
}
