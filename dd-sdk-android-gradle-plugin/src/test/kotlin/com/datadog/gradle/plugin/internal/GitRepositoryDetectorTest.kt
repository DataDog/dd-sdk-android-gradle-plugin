package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.Configurator
import com.datadog.gradle.plugin.RepositoryDetector
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.File
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class GitRepositoryDetectorTest {

    lateinit var testedDetector: RepositoryDetector

    @TempDir
    lateinit var tempDir: File

    @StringForgery(regex = "git@github\\.com:[a-z]+/[a-z][a-z0-9_-]+\\.git")
    lateinit var fakeRemoteUrl: String

    lateinit var fakeProject: Project

    lateinit var fakeSourceSetFolders: List<File>

    lateinit var fakeTrackedFiles: List<String>

    @BeforeEach
    fun `set up`(forge: Forge) {
        fakeProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        initializeSourceSets(forge)

        testedDetector = GitRepositoryDetector()
    }

    @Disabled("This doesn't perform well in the CI, but works locally")
    @Test
    fun `𝕄 return repository info 𝕎 detectRepository()`() {
        // Given
        initializeGit()

        // When
        val result = testedDetector.detectRepositories(fakeProject, fakeSourceSetFolders)

        // Then
        assertThat(result).hasSize(1)
        val repository = result.first()
        assertThat(repository.url).isEqualTo(fakeRemoteUrl)
        assertThat(repository.hash).isNotNull().isNotBlank()
        assertThat(repository.sourceFiles)
            .containsExactlyInAnyOrder(*fakeTrackedFiles.toTypedArray())
    }

    @Test
    fun `𝕄 return empty list 𝕎 detectRepository() { not inside a git repository }`() {
        // When
        val result = testedDetector.detectRepositories(fakeProject, fakeSourceSetFolders)

        // Then
        assertThat(result).hasSize(0)
    }

    private fun initializeSourceSets(forge: Forge) {
        val sourceSetFolders = mutableListOf<File>()
        val trackedFiles = mutableListOf<String>()
        for (i in 0..forge.anInt(1, 5)) {
            val sourceName = forge.anAlphabeticalString()
            val sourceSetFolder = File(fakeProject.rootDir, sourceName)
            sourceSetFolder.mkdirs()
            sourceSetFolders.add(sourceSetFolder)
            for (j in 0..forge.anInt(1, 5)) {
                val fileName = forge.aStringMatching("[a-z]{3,8}\\.[a-z]{3}")
                val file = File(sourceSetFolder, fileName)
                file.writeText(forge.anAlphabeticalString())
                trackedFiles.add("$sourceName${File.separator}$fileName")
            }
        }

        fakeSourceSetFolders = sourceSetFolders
        fakeTrackedFiles = trackedFiles
    }

    private fun initializeGit() {
        val readme = File(tempDir, "README.md")
        readme.writeText("# Fake project")

        check(
            ProcessBuilder("git", "init")
                .directory(tempDir)
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "add", ".")
                .directory(tempDir)
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "commit", "-m", "Init")
                .directory(tempDir)
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "remote", "add", "origin", fakeRemoteUrl)
                .directory(tempDir)
                .start()
                .waitFor(5, TimeUnit.SECONDS)
        )
    }
}
