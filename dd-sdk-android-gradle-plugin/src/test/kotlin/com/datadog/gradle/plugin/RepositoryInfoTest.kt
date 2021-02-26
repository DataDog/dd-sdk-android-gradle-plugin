package com.datadog.gradle.plugin

import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class RepositoryInfoTest {

    @Test
    fun `𝕄 serialize item to Json 𝕎 toJson()`(
        @Forgery repositoryInfo: RepositoryInfo
    ) {
        // When
        val result = repositoryInfo.toJson()

        // Then
        assertThat(result.getString(RepositoryInfo.KEY_REMOTE_URL))
            .isEqualTo(repositoryInfo.url)
        assertThat(result.getString(RepositoryInfo.KEY_COMMIT_HASH))
            .isEqualTo(repositoryInfo.hash)
        val trackedFiles = result.getJSONArray(RepositoryInfo.KEY_TRACKED_FILES).toList()
        assertThat(trackedFiles)
            .containsAll(repositoryInfo.sourceFiles)
            .hasSize(repositoryInfo.sourceFiles.size)
    }
}
