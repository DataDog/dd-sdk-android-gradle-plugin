/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.internal

/**
 * This Iterator will take a list of flavor name and trailing build type and iterate over all
 * possible (partial) variants from those.
 *
 * The partial variants will be iterated over in increasing order of priority.
 * Product flavors that belong to the first flavor dimension have a higher priority than
 * those belonging to the second flavor dimension.
 *
 * E.g.: for the variant with flavor names ["pro", "green", "release"], the iterator will
 * return the following values (in order):
 * "release", "green", "greenRelease", "pro", "proRelease", "proGreen", "proGreenRelease"
 */
internal class VariantIterator(
    private val names: List<String>
) : Iterator<String> {

    private var index = 1
    private val max = 1 shl names.size

    override fun hasNext(): Boolean {
        return index < max
    }

    override fun next(): String {
        if (index >= max) {
            throw NoSuchElementException()
        }

        // The idea of this algorithm is to generate all possible combination
        // while also keeping the order based on priorities.
        // Given a list of n names, we construct matching list of n booleans to decide
        // which of those name tokens we keep for the variant names.
        // Because the first name has a higher priority, the sequence would be (for 3 names):
        // false-false-true, false-true-false, false-true-true, true-false-false, and so on…
        // This sequence matches exactly the binary representation of integers starting from 1, so:
        // - the iterator iterates from 1 to (2^n - 1)
        // - at each step, we generate the binary representation of the current index
        // - we pad the binary with '0' to match the names size
        // - we convert the  binary representation string into a boolean array
        // - we zip the array with the name
        val mask = index.toString(2).padStart(names.size, '0').map { it == '1' }
        val filteredNames = mask.zip(names) { bool, string -> if (bool) string else null }
            .filterNotNull()

        index++

        return buildVariantName(filteredNames)
    }

    private fun buildVariantName(flavorNames: List<String>): String {
        return flavorNames.first() + flavorNames.drop(1).joinToString("") { it.capitalize() }
    }
}
