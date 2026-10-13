/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KotlinVersionTest {

    @Test
    fun `M return KOTLIN22 W give version 2_2_x and 2_3_x`() {
        assertThat(KotlinVersion.from("2.2.0")).isEqualTo(KotlinVersion.KOTLIN22)
        assertThat(KotlinVersion.from("2.2.10")).isEqualTo(KotlinVersion.KOTLIN22)
        assertThat(KotlinVersion.from("2.3.0")).isEqualTo(KotlinVersion.KOTLIN22)
        // Suffixes should be handled
        assertThat(KotlinVersion.from("2.2.0-alpha")).isEqualTo(KotlinVersion.KOTLIN22)
    }

    @Test
    fun `M return KOTLIN24 W give version 2_4_0 and newer`() {
        assertThat(KotlinVersion.from("2.4.0")).isEqualTo(KotlinVersion.KOTLIN24)
        assertThat(KotlinVersion.from("2.4.10")).isEqualTo(KotlinVersion.KOTLIN24)
        assertThat(KotlinVersion.from("2.5.0")).isEqualTo(KotlinVersion.KOTLIN24)
        assertThat(KotlinVersion.from("3.0.0")).isEqualTo(KotlinVersion.KOTLIN24)
        // Suffixes should be handled
        assertThat(KotlinVersion.from("2.4.0-alpha")).isEqualTo(KotlinVersion.KOTLIN24)
    }

    @Test
    fun `M return KOTLIN21 W give versions 2_1_x`() {
        assertThat(KotlinVersion.from("2.1.0")).isEqualTo(KotlinVersion.KOTLIN21)
        assertThat(KotlinVersion.from("2.1.20")).isEqualTo(KotlinVersion.KOTLIN21)
        assertThat(KotlinVersion.from("2.1.21")).isEqualTo(KotlinVersion.KOTLIN21)
        assertThat(KotlinVersion.from("2.1.99")).isEqualTo(KotlinVersion.KOTLIN21)
        // Suffixes should be handled
        assertThat(KotlinVersion.from("2.1.20-release-123")).isEqualTo(KotlinVersion.KOTLIN21)
    }

    @Test
    fun `M return KOTLIN20 W give versions 2_0_x`() {
        assertThat(KotlinVersion.from("2.0.0")).isEqualTo(KotlinVersion.KOTLIN20)
        assertThat(KotlinVersion.from("2.0.1")).isEqualTo(KotlinVersion.KOTLIN20)
        assertThat(KotlinVersion.from("2.0.99")).isEqualTo(KotlinVersion.KOTLIN20)
        // Suffixes should be handled
        assertThat(KotlinVersion.from("2.0.0-whatever")).isEqualTo(KotlinVersion.KOTLIN20)
    }

    @Test
    fun `M return KOTLIN19 W give versions 1_9_23 and newer`() {
        assertThat(KotlinVersion.from("1.9.23")).isEqualTo(KotlinVersion.KOTLIN19)
        assertThat(KotlinVersion.from("1.9.24")).isEqualTo(KotlinVersion.KOTLIN19)
        assertThat(KotlinVersion.from("1.9.99")).isEqualTo(KotlinVersion.KOTLIN19)
        // Suffixes should be handled
        assertThat(KotlinVersion.from("1.9.23-release-123")).isEqualTo(KotlinVersion.KOTLIN19)
    }

    @Test
    fun `M return UNSUPPORTED W give null versionString`() {
        assertThat(KotlinVersion.from(null)).isEqualTo(KotlinVersion.UNSUPPORTED)
    }

    @Test
    fun `M returns UNSUPPORTED W give malformed versionStrings`() {
        // Not enough parts
        assertThat(KotlinVersion.from("2.1")).isEqualTo(KotlinVersion.UNSUPPORTED)
        assertThat(KotlinVersion.from("2")).isEqualTo(KotlinVersion.UNSUPPORTED)
        assertThat(KotlinVersion.from("foo.bar.baz")).isEqualTo(KotlinVersion.UNSUPPORTED)

        // Empty string
        assertThat(KotlinVersion.from("")).isEqualTo(KotlinVersion.UNSUPPORTED)

        // Non-numeric parts
        assertThat(KotlinVersion.from("a.b.c")).isEqualTo(KotlinVersion.UNSUPPORTED)

        // Non-numeric patch part before stripping suffix
        assertThat(KotlinVersion.from("2.1.x-beta")).isEqualTo(KotlinVersion.UNSUPPORTED)
    }
}
