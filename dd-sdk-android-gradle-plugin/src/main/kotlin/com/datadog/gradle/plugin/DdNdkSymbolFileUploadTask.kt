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
import org.gradle.api.provider.Provider
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
    objectFactory: ObjectFactory,
    providerFactory: ProviderFactory,
    repositoryDetector: RepositoryDetector
) : DdFileUploadTask(providerFactory, repositoryDetector) {

    @get:InputFiles
    val searchDirectories: ConfigurableFileCollection = objectFactory.fileCollection()

    init {
        description =
            "Uploads NDK symbol files to Datadog servers to perform native crash symbolication."
    }

    override fun getFilesList(): List<Uploader.UploadFileInfo> {
        val files = mutableListOf<Uploader.UploadFileInfo>()

        searchDirectories
            .flatMap(this::findSoFiles)
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
            emptySet()
        }
    }

    companion object {
        internal const val TASK_NAME = "uploadNdkSymbolFiles"
        internal const val KEY_NDK_SYMBOL_FILE = "ndk_symbol_file"
        internal const val TYPE_NDK_SYMBOL_FILE = "ndk_symbol_file"
        internal const val ENCODING = "application/octet-stream"
        internal val SUPPORTED_ARCHS = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

        internal val LOGGER = Logging.getLogger("DdSymbolFileUploadTask")

        private fun getSearchDirs(
            buildTask: TaskProvider<ExternalNativeBuildTask>,
            providerFactory: ProviderFactory
        ): Provider<File?> {
            return buildTask.flatMap { task ->
                // var soFolder: `Provider
                @Suppress("MagicNumber")
                if (DdTaskUtils.isAgpAbove(8, 0, 0)) {
                    task.soFolder.map { it.asFile }
                } else {
                    val soFolder = ExternalNativeBuildTask::class.memberProperties.find {
                        it.name == "objFolder"
                    }?.get(task)
                    when (soFolder) {
                        is File -> providerFactory.provider { soFolder }
                        is DirectoryProperty -> soFolder.map { it.asFile }
                        else -> providerFactory.provider { null }
                    }
                }
            }
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
                LOGGER.info("No native build tasks found for variant ${variant.name}, skipping NDK symbol file upload.")
                return null
            }

            return project.tasks.register(
                TASK_NAME + variant.name.capitalize(),
                DdNdkSymbolFileUploadTask::class.java,
                repositoryDetector
            ).apply {
                configure { task ->
                    val roots = mutableListOf<File>()
                    variant.sourceSets.forEach {
                        roots.addAll(it.javaDirectories)
                        @Suppress("MagicNumber")
                        if (DdTaskUtils.isAgpAbove(7, 0, 0)) {
                            roots.addAll(it.kotlinDirectories)
                        }
                    }
                    task.sourceSetRoots = roots

                    nativeBuildProviders.forEach { buildTask ->
                        val searchFiles = getSearchDirs(buildTask, providerFactory)

                        task.searchDirectories.from(searchFiles)
                        task.dependsOn(buildTask)
                    }

                    task.datadogCiFile = DdTaskUtils.findDatadogCiFile(project.rootDir)
                    task.repositoryFile = DdTaskUtils.resolveDatadogRepositoryFile(project)
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
}
