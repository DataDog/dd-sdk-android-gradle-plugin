package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.util.UUID
import kotlin.io.path.Path

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
    @get:OutputFile
    val buildIdFile: Provider<RegularFile> = buildIdDirectory.file(BUILD_ID_FILE_NAME)

    init {
        outputs.upToDateWhen { false }
        group = DdAndroidGradlePlugin.DATADOG_TASK_GROUP
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
            variant: ApplicationVariant
        ): TaskProvider<GenerateBuildIdTask> {
            val variantName = variant.name.capitalize()
            val buildIdDirectory = target.layout.buildDirectory
                .dir(Path("generated", "datadog", "buildId", variant.name).toString())
            val generateBuildIdTask = target.tasks.register(
                TASK_NAME + variantName,
                GenerateBuildIdTask::class.java
            ) {
                it.buildIdDirectory.set(buildIdDirectory)
            }

            return generateBuildIdTask
        }
    }
}
