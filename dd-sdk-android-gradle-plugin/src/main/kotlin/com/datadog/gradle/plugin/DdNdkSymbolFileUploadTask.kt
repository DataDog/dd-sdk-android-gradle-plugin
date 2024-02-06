package com.datadog.gradle.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.tasks.ExternalNativeBuildTask
import com.datadog.gradle.plugin.internal.ApiKey
import com.datadog.gradle.plugin.internal.Uploader
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskProvider
import java.io.File
import javax.inject.Inject
import kotlin.reflect.full.memberProperties

/**
 * A Gradle task to upload NDK symbol files to Datadog servers.
 */
internal abstract class DdNdkSymbolFileUploadTask @Inject constructor(
    objects: ObjectFactory,
    providerFactory: ProviderFactory
) : DdFileUploadTask(providerFactory) {

    @get:InputFiles
    val searchDirectories: ConfigurableFileCollection = objects.fileCollection()

    init {
        description =
            "Uploads NDK symbol files to Datadog servers to perform native crash symbolication."
    }

    override fun getFilesList(): List<Uploader.UploadFileInfo> {
        val files = mutableListOf<Uploader.UploadFileInfo>()

        searchDirectories
            .flatMap(this::findSoFiles)
            .toSortedSet(compareBy { it.absolutePath })
            .toSet()
            .forEach { file ->
                val arch = file.parentFile.name
                require(SUPPORTED_ARCHS.contains(arch))
                files.add(
                    Uploader.UploadFileInfo(
                        KEY_NDK_SYMBOL_FILE,
                        file,
                        encoding = ENCODING,
                        TYPE_NDK_SYMBOL_FILE,
                        file.name,
                        mapOf(
                            "arch" to arch
                        )
                    )
                )
            }

        return files
    }

    private fun findSoFiles(searchDirectory: File): Collection<File> {
        return if (searchDirectory.exists() && searchDirectory.isDirectory) {
            searchDirectory.walkTopDown()
                .filter { it.extension == "so" }
                .toSet()
        } else {
            return emptySet()
        }
    }

    companion object {
        internal const val TASK_NAME = "ddUploadNdkSymbolFiles"
        internal const val KEY_NDK_SYMBOL_FILE = "ndk_symbol_file"
        internal const val TYPE_NDK_SYMBOL_FILE = "ndk_symbol_file"
        internal const val ENCODING = "application/octet-stream"
        internal val SUPPORTED_ARCHS = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        internal const val MAX_DATADOG_CI_FILE_LOOKUP_LEVELS = 4

        internal val LOGGER = Logging.getLogger("DdSymbolFileUploadTask")

        @Suppress("StringLiteralDuplication")
        private fun resolveDatadogRepositoryFile(target: Project): File {
            val outputsDir = File(target.buildDir, "outputs")
            val reportsDir = File(outputsDir, "reports")
            val datadogDir = File(reportsDir, "datadog")
            return File(datadogDir, "repository.json")
        }

        internal fun findDatadogCiFile(projectDir: File): File? {
            var currentDir: File? = projectDir
            var levelsUp = 0
            while (currentDir != null && levelsUp < MAX_DATADOG_CI_FILE_LOOKUP_LEVELS) {
                val datadogCiFile = File(currentDir, "datadog-ci.json")
                if (datadogCiFile.exists()) {
                    return datadogCiFile
                }
                currentDir = currentDir.parentFile
                levelsUp++
            }
            return null
        }

        @Suppress("LongParameterList", "ReturnCount")
        fun register(
            project: Project,
            variant: ApplicationVariant,
            buildIdTask: TaskProvider<GenerateBuildIdTask>,
            providerFactory: ProviderFactory,
            apiKey: ApiKey,
            extensionConfiguration: DdExtensionConfiguration,
            repositoryDetector: RepositoryDetector
        ): TaskProvider<DdNdkSymbolFileUploadTask>? {
            val nativeBuildProviders = variant.externalNativeBuildProviders
            if (nativeBuildProviders.isEmpty()) {
                LOGGER.info("No native build tasks found for variant ${variant.name}, skipping symbol file upload.")
                return null
            }

            return project.tasks.register(
                TASK_NAME + variant.name.capitalize(),
                DdNdkSymbolFileUploadTask::class.java
            ) { task ->

                nativeBuildProviders.forEach { buildTask ->
                    val searchFiles = buildTask.flatMap {
                        it.soFolder
                    }

                    task.searchDirectories.from(searchFiles)
                    task.dependsOn(buildTask)
                }

                task.datadogCiFile = findDatadogCiFile(project.rootDir)
                task.repositoryDetector = repositoryDetector
                task.repositoryFile = resolveDatadogRepositoryFile(project)
                task.configureWith(
                    apiKey,
                    extensionConfiguration,
                    variant
                )

                task.buildId = buildIdTask.flatMap {
                    it.buildIdFile.flatMap {
                        providerFactory.provider { it.asFile.readText().trim() }
                    }
                }
            }
        }
    }
}
