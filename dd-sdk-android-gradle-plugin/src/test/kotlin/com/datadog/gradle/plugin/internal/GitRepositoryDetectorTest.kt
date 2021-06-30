package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.Configurator
import com.datadog.gradle.plugin.RepositoryDetector
import com.datadog.gradle.plugin.internal.sanitizer.UrlSanitizer
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.File
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.process.internal.DefaultExecOperations
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
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

    @StringForgery(regex = "http[s]?://github\\.com:[1-9]{2}/[a-z]+/repository\\.git")
    lateinit var fakeRemoteUrl: String

    @StringForgery(regex = "http[s]?://github\\.com:[1-9]{2}/[a-z]+/repository\\.git")
    lateinit var fakeSanitizedUrl: String

    lateinit var fakeProject: Project

    lateinit var fakeSourceSetFolders: List<File>

    lateinit var fakeTrackedFiles: List<String>

    @Mock
    lateinit var mockedUrlSanitizer: UrlSanitizer

    @Suppress("UnstableApiUsage")
    @BeforeEach
    fun `set up`(forge: Forge) {
        fakeProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        initializeSourceSets(forge)
        whenever(mockedUrlSanitizer.sanitize(fakeRemoteUrl)).thenReturn(fakeSanitizedUrl)
        testedDetector = GitRepositoryDetector(
            DefaultExecOperations((fakeProject as DefaultProject).processOperations),
            mockedUrlSanitizer
        )
    }

    @Test
    fun `𝕄 return repository info 𝕎 detectRepository()`() {
        // Given
        initializeGit()

        // When
        val result = testedDetector.detectRepositories(fakeSourceSetFolders)

        // Then
        assertThat(result).hasSize(1)
        val repository = result.first()
        assertThat(repository.url).isEqualTo(fakeSanitizedUrl)
        assertThat(repository.hash).isNotNull().isNotBlank()
        assertThat(repository.sourceFiles)
            .containsExactlyInAnyOrder(*fakeTrackedFiles.toTypedArray())
    }

    @Test
    fun `𝕄 use the sanitized config remote URL 𝕎 detectRepository() { remote URL provided }`(
        @StringForgery(regex = "https://[a-z]{4,10}\\.[com|org]/[a-z]{4,10}/[a-z]{4,10}\\.git")
        fakeConfigRemoteUrl: String,
        @StringForgery(regex = "https://[a-z]{4,10}\\.[com|org]/[a-z]{4,10}/[a-z]{4,10}\\.git")
        fakeSanitizedConfigRemoteUrl: String
    ) {
        // Given
        initializeGit()
        whenever(mockedUrlSanitizer.sanitize(fakeConfigRemoteUrl)).thenReturn(
            fakeSanitizedConfigRemoteUrl
        )

        // When
        val result = testedDetector.detectRepositories(
            fakeSourceSetFolders,
            fakeConfigRemoteUrl
        )

        // Then
        assertThat(result).hasSize(1)
        val repository = result.first()
        assertThat(repository.url).isEqualTo(fakeSanitizedConfigRemoteUrl)
        assertThat(repository.hash).isNotNull().isNotBlank()
        assertThat(repository.sourceFiles)
            .containsExactlyInAnyOrder(*fakeTrackedFiles.toTypedArray())
    }

    @Test
    fun `𝕄 return empty list 𝕎 detectRepository() { not inside a git repository }`() {
        // When
        val result = testedDetector.detectRepositories(fakeSourceSetFolders)

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

    private fun initializeGit(remoteUrl: String = fakeRemoteUrl) {
        val readme = File(tempDir, "README.md")
        readme.writeText("# Fake project")

        check(
            ProcessBuilder("git", "init")
                .directory(tempDir)
                .start()
                .waitForSuccess(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "add", ".")
                .directory(tempDir)
                .start()
                .waitForSuccess(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "config", "user.name", "\"Some User\"")
                .directory(tempDir)
                .start()
                .waitForSuccess(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "config", "user.email", "\"user@example.com\"")
                .directory(tempDir)
                .start()
                .waitForSuccess(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "commit", "-m", "Init")
                .directory(tempDir)
                .start()
                .waitForSuccess(5, TimeUnit.SECONDS)
        )
        check(
            ProcessBuilder("git", "remote", "add", "origin", remoteUrl)
                .directory(tempDir)
                .start()
                .waitForSuccess(5, TimeUnit.SECONDS)
        )
    }

    private fun Process.waitForSuccess(timeout: Long, unit: TimeUnit): Boolean {
        val haveNoTimeout = waitFor(timeout, unit)
        return haveNoTimeout && exitValue() == 0
    }
}
