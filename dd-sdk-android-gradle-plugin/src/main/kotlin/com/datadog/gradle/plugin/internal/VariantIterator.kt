package com.datadog.gradle.plugin.internal

/**
 * This Iterator will take a list of flavor name and iterate over all possible
 * (partial) variants from those.
 *
 * The partial variants will be iterated over in increasing order of priority.
 * Product flavors that belong to the first flavor dimension have a higher priority than
 * those belonging to the second flavor dimension.
 *
 * E.g.: for the variant with flavor names ["pro", "green", "europe"], the iterator will
 * return the following values (in order):
 * "europe", "green", "greenEurope", "pro", "proEurope", "proGreen", "proGreenEurope"
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
        val mask = index.toString(2).padStart(names.size, '0').map { it == '1' }
        val filteredNames = mask.zip(names) { b, s -> if (b) s else null }.filterNotNull()
        index++
        return filteredNames.first() + filteredNames.drop(1).joinToString("") { it.capitalize() }
    }
}
