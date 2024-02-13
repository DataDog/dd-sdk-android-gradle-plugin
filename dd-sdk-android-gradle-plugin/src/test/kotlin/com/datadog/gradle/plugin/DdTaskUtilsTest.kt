package com.datadog.gradle.plugin

import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import java.io.File

@Extensions(
    ExtendWith(ForgeExtension::class)
)
@ForgeConfiguration(Configurator::class)
class DdTaskUtilsTest {
    @Test
    fun `𝕄 find datadog-ci file 𝕎 findDatadogCiFile()`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, maxDepth = 3, forge = forge)

        File(tree[forge.anInt(0, tree.size)], "datadog-ci.json").createNewFile()

        // When
        val ciFile = DdTaskUtils.findDatadogCiFile(tree.last())

        // Then
        Assertions.assertThat(ciFile).isNotNull()
    }

    @Test
    fun `𝕄 return null 𝕎 findDatadogCiFile() { no ci file found }`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, maxDepth = 3, forge = forge)

        // When
        val ciFile = DdTaskUtils.findDatadogCiFile(tree.last())

        // Then
        Assertions.assertThat(ciFile).isNull()
    }

    @Test
    fun `𝕄 return null 𝕎 findDatadogCiFile() { beyond max levels up }`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, minDepth = 4, maxDepth = 7, forge = forge)

        // When
        val ciFile = DdTaskUtils.findDatadogCiFile(tree.last())

        // Then
        Assertions.assertThat(ciFile).isNull()
    }

    private fun buildDirectoryTree(
        rootDir: File,
        minDepth: Int = 1,
        maxDepth: Int,
        forge: Forge
    ): List<File> {
        var currentDir = rootDir
        val tree = mutableListOf(rootDir)
        val stopAt = forge.anInt(min = minDepth, max = maxDepth + 1)
        for (level in 1..maxDepth) {
            if (level == stopAt) break

            currentDir = File(currentDir, forge.anAlphabeticalString())
            tree.add(currentDir)
        }

        tree.last().mkdirs()

        return tree
    }
}
