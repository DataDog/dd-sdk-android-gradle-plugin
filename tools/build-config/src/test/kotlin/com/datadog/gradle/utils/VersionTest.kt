/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VersionTest {

    @Test
    fun checkMajorInRange() {
        assertThrows<IllegalArgumentException> {
            Version(Version.MAX_MAJOR, 0, 0)
        }
    }

    @Test
    fun checkMinorInRange() {
        assertThrows<IllegalArgumentException> {
            Version(0, Version.MAX_MINOR, 0)
        }
    }

    @Test
    fun checkHotfixInRange() {
        assertThrows<IllegalArgumentException> {
            Version(0, 0, Version.MAX_HOTFIX)
        }
    }

    @Test
    fun computesName() {
        // When
        val name = Version(3, 12, 7).name

        // Then
        assertThat(name).isEqualTo("3.12.7")
    }

    @Test
    fun computesCode() {
        // When
        val code = Version(3, 12, 7).code

        // Then
        assertThat(code).isEqualTo(3127)
    }

    @Test
    fun ensureCodeSequenceHotfix() {
        // When
        val code = Version(3, 12, Version.MAX_HOTFIX - 1).code
        val next = Version(3, 13, 0).code

        // Then
        assertThat(code).isEqualTo(next - 1)
    }

    @Test
    fun ensureCodeSequenceMinor() {
        // When
        val code = Version(3, Version.MAX_MINOR - 1, Version.MAX_HOTFIX - 1).code
        val next = Version(4, 0, 0).code

        // Then
        assertThat(code).isEqualTo(next - 1)
    }

    @Test
    fun addNoSuffixForRelease() {
        // When
        val name = Version(3, 12, 7, Version.Type.Release).name

        // Then
        assertThat(name).isEqualTo("3.12.7")
    }

    @Test
    fun addSuffixForRC() {
        // When
        val name = Version(3, 12, 7, Version.Type.ReleaseCandidate(1)).name

        // Then
        assertThat(name).isEqualTo("3.12.7-rc1")
    }

    @Test
    fun addSuffixForBeta() {
        // When
        val name = Version(3, 12, 7, Version.Type.Beta(5)).name

        // Then
        assertThat(name).isEqualTo("3.12.7-beta5")
    }

    @Test
    fun addSuffixForAlpha() {
        // When
        val name = Version(3, 12, 7, Version.Type.Alpha(3)).name

        // Then
        assertThat(name).isEqualTo("3.12.7-alpha3")
    }

    @Test
    fun addSuffixForDev() {
        // When
        val name = Version(3, 12, 7, Version.Type.Dev).name

        // Then
        assertThat(name).isEqualTo("3.12.7-dev")
    }

    @Test
    fun addSuffixForSnapshot() {
        // When
        val name = Version(3, 12, 7, Version.Type.Snapshot).name

        // Then
        assertThat(name).isEqualTo("3.12.7-SNAPSHOT")
    }
}
