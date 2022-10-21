/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

import com.datadog.gradle.plugin.Configurator
import com.datadog.gradle.plugin.utils.capitalizeChar
import fr.xgouchet.elmyr.Case
import fr.xgouchet.elmyr.annotation.StringForgery
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
internal class VariantIteratorTest {

    @Test
    fun `𝕄 iterate 𝕎 forEach() { 1 dimension }`(
        @StringForgery(case = Case.LOWER) a: String
    ) {
        // Given
        val iterator = VariantIterator(listOf(a))

        // When
        val output = mutableListOf<String>()
        iterator.forEach { output.add(it) }

        // Then
        assertThat(output)
            .containsExactly(a)
    }

    @Test
    fun `𝕄 iterate 𝕎 forEach() { 2 dimensions }`(
        @StringForgery(case = Case.LOWER) a: String,
        @StringForgery(case = Case.LOWER) b: String
    ) {
        // Given
        val B = b.replaceFirstChar { capitalizeChar(it) }
        val iterator = VariantIterator(listOf(a, b))

        // When
        val output = mutableListOf<String>()
        iterator.forEach { output.add(it) }

        // Then
        assertThat(output)
            .containsExactly(b, a, a + B)
    }

    @Test
    fun `𝕄 iterate 𝕎 forEach() { 3 dimensions }`(
        @StringForgery(case = Case.LOWER) a: String,
        @StringForgery(case = Case.LOWER) b: String,
        @StringForgery(case = Case.LOWER) c: String
    ) {
        // Given
        val B = b.replaceFirstChar { capitalizeChar(it) }
        val C = c.replaceFirstChar { capitalizeChar(it) }
        val iterator = VariantIterator(listOf(a, b, c))

        // When
        val output = mutableListOf<String>()
        iterator.forEach { output.add(it) }

        // Then
        assertThat(output)
            .containsExactly(c, b, b + C, a, a + C, a + B, a + B + C)
    }

    @Test
    fun `𝕄 iterate 𝕎 forEach() { 4 dimensions }`(
        @StringForgery(case = Case.LOWER) a: String,
        @StringForgery(case = Case.LOWER) b: String,
        @StringForgery(case = Case.LOWER) c: String,
        @StringForgery(case = Case.LOWER) d: String
    ) {
        // Given
        val B = b.replaceFirstChar { capitalizeChar(it) }
        val C = c.replaceFirstChar { capitalizeChar(it) }
        val D = d.replaceFirstChar { capitalizeChar(it) }
        val iterator = VariantIterator(listOf(a, b, c, d))

        // When
        val output = mutableListOf<String>()
        iterator.forEach { output.add(it) }

        // Then
        assertThat(output)
            .containsExactly(
                d, c, c + D,
                b, b + D, b + C, b + C + D,
                a, a + D, a + C, a + C + D,
                a + B, a + B + D, a + B + C,
                a + B + C + D
            )
    }

    @Test
    fun `𝕄 iterate 𝕎 forEach() { anySize }`(
        @StringForgery(case = Case.LOWER) names: List<String>
    ) {
        // Given
        // filtering is needed, because say there are 2 names: ["a", "bab"]. In this example
        // assertion of allMatch below will fail, because "bab" contains "a" variant name,
        // but doesn't start with it (as we would expect in case of combination)
        val limitedNames = names.filter { item -> !names.any { it != item && it.contains(item) } }
            .take(10) // limit the complexity of the data
        val iterator = VariantIterator(limitedNames)

        // When
        val output = mutableListOf<String>()
        iterator.forEach { output.add(it) }

        // Then
        val firstFlavor = limitedNames.first()
        val lastFlavor = limitedNames.last()
        val lastFlavorCapitalized = lastFlavor.replaceFirstChar { capitalizeChar(it) }
        assertThat(output.first()).isEqualTo(lastFlavor)
        assertThat(output.last()).isEqualTo(
            firstFlavor + limitedNames.drop(1).joinToString("") {
                it.replaceFirstChar { capitalizeChar(it) }
            }
        )
        assertThat(output).allMatch {
            // first flavor always appear first
            if (it.contains(firstFlavor)) {
                it.startsWith(firstFlavor)
            } else {
                true
            }
        }.allMatch {
            // last flavor always appear last
            if (it.contains(lastFlavorCapitalized)) {
                it.endsWith(lastFlavorCapitalized)
            } else if (it.contains(lastFlavor)) {
                it == (lastFlavor)
            } else {
                true
            }
        }
    }
}
