package com.datadog.gradle.plugin.kcp

import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinVersionTest {

    @Test
    fun `M return KOTLIN22 W give version 2_2_0 and newer`() {
        assertEquals(KotlinVersion.KOTLIN22, KotlinVersion.from("2.2.0"))
        assertEquals(KotlinVersion.KOTLIN22, KotlinVersion.from("2.2.10"))
        assertEquals(KotlinVersion.KOTLIN22, KotlinVersion.from("2.3.0"))
        assertEquals(KotlinVersion.KOTLIN22, KotlinVersion.from("3.0.0"))
        assertEquals(
            KotlinVersion.KOTLIN22,
            KotlinVersion.from("2.2.0-alpha")
        ) // Suffixes should be handled
    }

    @Test
    fun `M return KOTLIN21 W give versions from 2_1_20 up to (but not including) 2_2_0`() {
        assertEquals(KotlinVersion.KOTLIN21, KotlinVersion.from("2.1.20"))
        assertEquals(KotlinVersion.KOTLIN21, KotlinVersion.from("2.1.21"))
        assertEquals(KotlinVersion.KOTLIN21, KotlinVersion.from("2.1.99"))
        assertEquals(KotlinVersion.KOTLIN21, KotlinVersion.from("2.1.20-release-123"))
    }

    @Test
    fun `M return KOTLIN20 W give versions older than 2_1_20`() {
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from("2.1.19"))
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from("2.0.0"))
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from("1.9.23"))
        assertEquals(
            KotlinVersion.KOTLIN20,
            KotlinVersion.from("2.1.19-whatever")
        ) // Suffixes should be handled
    }

    @Test
    fun `M return KOTLIN20 W give null versionString`() {
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from(null))
    }

    @Test
    fun `M returns KOTLIN20 W give malformed versionStrings`() {
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from("2.1")) // Not enough parts
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from("2")) // Not enough parts
        assertEquals(
            KotlinVersion.KOTLIN20,
            KotlinVersion.from("foo.bar.baz")
        ) // Non-numeric parts
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from("")) // Empty string
        assertEquals(KotlinVersion.KOTLIN20, KotlinVersion.from("a.b.c")) // Non-numeric parts
        assertEquals(
            KotlinVersion.KOTLIN20,
            KotlinVersion.from("2.1.x-beta")
        ) // Non-numeric patch part before stripping suffix
    }
}
