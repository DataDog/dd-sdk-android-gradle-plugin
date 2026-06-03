/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language

object KotlinCompilerTestSources {

    const val TEST_CALLBACK_CONTAINER_PATH = "com.datadog.gradle.plugin.kcp.TestCallbackContainer"
    const val NAV_GRAPH_BUILDER_PATH = "androidx.navigation.compose.NavGraphBuilder"
    const val NAV_HOST_BUILDER_PATH = "androidx.navigation.compose.NavHost"
    const val NAV_HOST_CONTROLLER_PATH = "androidx.navigation.NavHostController"
    const val TRACKING_EFFECT_FILE_NAME = "Navigation.kt"
    const val DD_MODIFIER_CLASS_FILE_NAME = "DatadogModifier.kt"
    const val COMPOSE_INSTRUMENTATION_ANNOTATION_FILE_NAME = "ComposeInstrumentation.kt"

    @Language("kotlin")
    val DD_MODIFIER_SOURCE_FILE_CONTENT =
        """
        package com.datadog.android.compose

        import androidx.compose.ui.Modifier
        import androidx.compose.ui.semantics.SemanticsPropertyKey
        import androidx.compose.ui.semantics.SemanticsPropertyReceiver
        import androidx.compose.ui.semantics.semantics
        import com.datadog.gradle.plugin.kcp.TestCallbackContainer

        fun Modifier.instrumentedDatadog(name: String, isImageRole: Boolean = false): Modifier {
            TestCallbackContainer.invokeCallback(isImageRole)
            return this.semantics {
                this.datadog = name
            }
        }

        internal val DatadogSemanticsPropertyKey: SemanticsPropertyKey<String> = SemanticsPropertyKey(
            name = "_dd_semantics",
            mergePolicy = { parentValue, childValue ->
                parentValue
            }
        )

        private var SemanticsPropertyReceiver.datadog by DatadogSemanticsPropertyKey
        """.trimIndent()

    @Language("kotlin")
    val TRACKING_EFFECT_SOURCE_FILE_CONTENT =
        """
        package com.datadog.android.compose

        import androidx.compose.runtime.Composable
        import androidx.navigation.NavController
        import androidx.navigation.NavDestination
        import com.datadog.gradle.plugin.kcp.TestCallbackContainer


        @Composable
        fun InstrumentedNavigationViewTrackingEffect(
            navController: NavController
        ) {
            TestCallbackContainer.invokeCallback()
        }
        """.trimIndent()

    @Language("kotlin")
    val COMPOSE_INSTRUMENTATION_ANNOTATION_SOURCE_FILE_CONTENT =
        """
        package com.datadog.android.compose

        @Retention(AnnotationRetention.BINARY)
        annotation class ComposeInstrumentation

        """.trimIndent()

    val trackingEffectSourceFile = SourceFile.kotlin(
        TRACKING_EFFECT_FILE_NAME,
        TRACKING_EFFECT_SOURCE_FILE_CONTENT
    )

    val datadogModifierSourceFile = SourceFile.kotlin(
        DD_MODIFIER_CLASS_FILE_NAME,
        DD_MODIFIER_SOURCE_FILE_CONTENT
    )

    val composeInstrumentationAnnotationSourceFile = SourceFile.kotlin(
        COMPOSE_INSTRUMENTATION_ANNOTATION_FILE_NAME,
        COMPOSE_INSTRUMENTATION_ANNOTATION_SOURCE_FILE_CONTENT
    )

    // TODO RUM-8950: Dependency file should be separated in each file after the DSL configuration is introduced.
    val dependencyFiles = listOf(
        trackingEffectSourceFile,
        datadogModifierSourceFile,
        composeInstrumentationAnnotationSourceFile
    )
}
