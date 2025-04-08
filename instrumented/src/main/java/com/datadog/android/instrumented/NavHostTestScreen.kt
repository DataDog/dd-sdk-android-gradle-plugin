package com.datadog.android.instrumented

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
internal fun ScreenWithNavHost(onEvent: (NavHostController, Lifecycle.Event) -> Unit) {
    val navHost = rememberNavController()
    NavHost(navHost, TEST_DESTINATION) {
        composable(TEST_DESTINATION) {
            Text(text = TEST_TEXT)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            onEvent(navHost, event)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
internal fun ScreenWithNavHostNested(onEvent: (NavHostController, Lifecycle.Event) -> Unit) {
    Column {
        Row {
            val navHost = rememberNavController()
            NavHost(navHost, TEST_DESTINATION) {
                composable(TEST_DESTINATION) {
                    Text(text = TEST_TEXT)
                }
            }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { source, event ->
                    onEvent(navHost, event)
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }
    }
}

internal const val TEST_TEXT = "ScreenWithNavHost"
internal const val TEST_DESTINATION = "destination"
