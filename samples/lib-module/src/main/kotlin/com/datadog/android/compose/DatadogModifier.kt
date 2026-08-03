package com.datadog.android.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Function to simulate production function in SDK code base for instrumented testing purpose.
 * This function should have exactly the same package name, function signature and return type
 * with the production one.
 *
 * Mirrors the production node-based implementation (see #3661): equality is keyed on the semantics
 * inputs so the injected modifier compares equal across recompositions and does not break skipping.
 */
internal fun Modifier.instrumentedDatadog(name: String, isImageRole: Boolean = false): Modifier {
    return this then DatadogSemanticsElement(name, isImageRole)
}

private class DatadogSemanticsElement(
    private val name: String,
    private val isImageRole: Boolean
) : ModifierNodeElement<DatadogSemanticsNode>(), SemanticsModifier {

    override val semanticsConfiguration: SemanticsConfiguration
        get() = SemanticsConfiguration().apply {
            datadog = name
            if (isImageRole) {
                this[SemanticsProperties.Role] = Role.Image
            }
        }

    override fun create(): DatadogSemanticsNode = DatadogSemanticsNode(name, isImageRole)

    override fun update(node: DatadogSemanticsNode) {
        node.update(name, isImageRole)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DatadogSemanticsElement) return false
        return name == other.name && isImageRole == other.isImageRole
    }

    override fun hashCode(): Int = HASH_MULTIPLIER * name.hashCode() + isImageRole.hashCode()

    private companion object {
        private const val HASH_MULTIPLIER = 31
    }
}

private class DatadogSemanticsNode(
    private var name: String,
    private var isImageRole: Boolean
) : Modifier.Node(), SemanticsModifierNode {

    fun update(name: String, isImageRole: Boolean) {
        this.name = name
        this.isImageRole = isImageRole
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this.datadog = name
        if (isImageRole) {
            this[SemanticsProperties.Role] = Role.Image
        }
    }
}

internal val DatadogSemanticsPropertyKey: SemanticsPropertyKey<String> = SemanticsPropertyKey(
    name = "_dd_semantics",
    mergePolicy = { parentValue, _ ->
        parentValue
    }
)

private var SemanticsPropertyReceiver.datadog by DatadogSemanticsPropertyKey
