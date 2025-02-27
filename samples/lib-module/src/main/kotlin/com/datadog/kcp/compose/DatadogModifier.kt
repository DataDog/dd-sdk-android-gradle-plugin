package com.datadog.kcp.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/**
 * Function to simulate production function in SDK code base for instrumented testing purpose.
 * This function should have exactly the same package name, function signature and return type
 * with the production one.
 */
fun Modifier.datadog(name: String): Modifier {
    return this.semantics {
        this.datadog = name
    }
}

internal val DatadogSemanticsPropertyKey: SemanticsPropertyKey<String> = SemanticsPropertyKey(
    name = "_dd_semantics",
    mergePolicy = { parentValue, _ ->
        parentValue
    }
)

private var SemanticsPropertyReceiver.datadog by DatadogSemanticsPropertyKey
