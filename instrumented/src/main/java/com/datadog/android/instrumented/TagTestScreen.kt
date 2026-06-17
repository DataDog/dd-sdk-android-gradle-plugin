package com.datadog.android.instrumented

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun ScreenWithoutModifier() {
    Column {
        Text(
            text = "ScreenWithoutModifier"
        )
    }
}

@Composable
internal fun ScreenWithDefaultModifier() {
    Column(modifier = Modifier) {
        Text(
            modifier = Modifier,
            text = "ScreenWithDefaultModifier"
        )
    }
}

@Composable
internal fun ScreenWithCustomModifier() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Red)
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "ScreenWithCustomModifier"
        )
    }
}

/**
 * Reproduces issue #575 for a nullable Modifier default consumed via an elvis fallback.
 *
 * [BoxWithNullableModifierDefault] is called without [Modifier], so it relies on its `null` default. The
 * instrumentation must NOT overwrite that omitted argument: if it did, `iconModifier` would become non-null,
 * the elvis would short-circuit, and the fallback [Modifier.testTag] would be dropped. The presence of the
 * [NULLABLE_DEFAULT_TAG] node proves the default is preserved.
 */
@Composable
internal fun ScreenWithNullableModifierDefault() {
    BoxWithNullableModifierDefault()
}

@Composable
private fun BoxWithNullableModifierDefault(iconModifier: Modifier? = null) {
    Box(
        modifier = (iconModifier ?: Modifier.testTag(NULLABLE_DEFAULT_TAG)).size(40.dp)
    ) {}
}

/**
 * Reproduces issue #575 for a non-null, behavior-carrying Modifier default.
 *
 * [BoxWithSizingModifierDefault] is called without [Modifier], so it relies on its
 * `Modifier.testTag(...).size(...)` default. The instrumentation must NOT overwrite that omitted argument,
 * otherwise the sizing/tag default is discarded. The presence of the [SIZING_DEFAULT_TAG] node proves it.
 */
@Composable
internal fun ScreenWithSizingModifierDefault() {
    BoxWithSizingModifierDefault()
}

@Composable
private fun BoxWithSizingModifierDefault(
    iconModifier: Modifier = Modifier.testTag(SIZING_DEFAULT_TAG).size(40.dp)
) {
    Box(modifier = iconModifier) {}
}

internal const val NULLABLE_DEFAULT_TAG = "nullable_default_preserved"
internal const val SIZING_DEFAULT_TAG = "sizing_default_preserved"
