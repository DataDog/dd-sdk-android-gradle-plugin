package com.datadog.android.instrumented

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
