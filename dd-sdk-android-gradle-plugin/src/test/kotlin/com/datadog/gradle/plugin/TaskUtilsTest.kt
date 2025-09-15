/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.utils.forge.Configurator
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.util.stream.Stream

@Extensions(
    ExtendWith(ForgeExtension::class)
)
@ForgeConfiguration(Configurator::class)
class TaskUtilsTest {

    @Test
    fun `M find datadog-ci file W findDatadogCiFile()`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, maxDepth = 3, forge = forge)

        File(tree[forge.anInt(0, tree.size)], "datadog-ci.json").createNewFile()

        // When
        val ciFile = TaskUtils.findDatadogCiFile(tree.last())

        // Then
        assertThat(ciFile).isNotNull()
    }

    @Test
    fun `M return null W findDatadogCiFile() { no ci file found }`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, maxDepth = 3, forge = forge)

        // When
        val ciFile = TaskUtils.findDatadogCiFile(tree.last())

        // Then
        assertThat(ciFile).isNull()
    }

    @Test
    fun `M return null W findDatadogCiFile() { beyond max levels up }`(
        @TempDir rootDir: File,
        forge: Forge
    ) {
        // Given
        val tree = buildDirectoryTree(rootDir, minDepth = 4, maxDepth = 7, forge = forge)

        // When
        val ciFile = TaskUtils.findDatadogCiFile(tree.last())

        // Then
        assertThat(ciFile).isNull()
    }

    @ParameterizedTest
    @MethodSource("versionsBelow")
    fun `M return false W isVersionEqualOrAbove() { version is below }`(
        left: SemVer,
        right: SemVer
    ) {
        // When
        val isEqualOrAbove = TaskUtils.isVersionEqualOrAbove(left.toString(), right.major, right.minor, right.patch)

        // Then
        assertThat(isEqualOrAbove).isFalse
    }

    @ParameterizedTest
    @MethodSource("versionsAbove")
    fun `M return true W isVersionEqualOrAbove() { version is above }`(
        left: SemVer,
        right: SemVer
    ) {
        // When
        val isEqualOrAbove = TaskUtils.isVersionEqualOrAbove(left.toString(), right.major, right.minor, right.patch)

        // Then
        assertThat(isEqualOrAbove).isTrue
    }

    @Test
    fun `M return true W isVersionEqualOrAbove() { versions are equal }`(
        forge: Forge
    ) {
        // Given
        val major = forge.aPositiveInt()
        val minor = forge.aPositiveInt()
        val patch = forge.aPositiveInt()

        // When
        val isEqualOrAbove = TaskUtils.isVersionEqualOrAbove("$major.$minor.$patch", major, minor, patch)

        // Then
        assertThat(isEqualOrAbove).isTrue
    }

    @Test
    fun `M return false W isVersionEqualOrAbove() { empty string }`(
        forge: Forge
    ) {
        // Given
        val major = forge.aPositiveInt()
        val minor = forge.aPositiveInt()
        val patch = forge.aPositiveInt()

        // When
        val isEqualOrAbove = TaskUtils.isVersionEqualOrAbove("", major, minor, patch)

        // Then
        assertThat(isEqualOrAbove).isFalse
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

    class SemVer(val major: Int, val minor: Int, val patch: Int, val suffix: String = "") {
        override fun toString(): String {
            return "$major.$minor.$patch${if (suffix.isNotEmpty()) "-$suffix" else ""}"
        }
    }

    companion object {
        @ValueSource
        @JvmStatic
        fun versionsBelow(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(SemVer(1, 2, 3), SemVer(2, 2, 3)),
                Arguments.of(SemVer(1, 2, 3, "rc1"), SemVer(2, 2, 3)),
                Arguments.of(SemVer(1, 2, 3), SemVer(1, 3, 3)),
                Arguments.of(SemVer(1, 2, 3), SemVer(1, 2, 4))
            )
        }

        @ValueSource
        @JvmStatic
        fun versionsAbove(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(SemVer(1, 2, 3), SemVer(0, 2, 3)),
                Arguments.of(SemVer(1, 2, 3), SemVer(1, 1, 3)),
                Arguments.of(SemVer(1, 2, 3), SemVer(1, 2, 2)),
                Arguments.of(SemVer(1, 2, 3, "rc1"), SemVer(1, 2, 2))
            )
        }
    }
}
