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
    const val SDK_CORE_FILE_NAME = "SdkCore.kt"
    const val COMPONENT_PREDICATE_FILE_NAME = "ComponentPredicate.kt"
    const val ACCEPT_ALL_NAV_DESTINATIONS_FILE_NAME = "AcceptAllNavDestinations.kt"
    const val DATADOG_FILE_NAME = "Datadog.kt"

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
        import com.datadog.android.api.SdkCore
        import com.datadog.android.Datadog
        import com.datadog.android.rum.tracking.AcceptAllNavDestinations
        import com.datadog.android.rum.tracking.ComponentPredicate
        import com.datadog.gradle.plugin.kcp.TestCallbackContainer

        @Composable
        fun InstrumentedNavigationViewTrackingEffect(
            navController: NavController,
            trackArguments: Boolean = true,
            destinationPredicate: ComponentPredicate<NavDestination> = AcceptAllNavDestinations(),
            sdkCore: SdkCore = Datadog.getInstance()
        ) {
            TestCallbackContainer.invokeCallback()
        }
        """.trimIndent()

    @Language("kotlin")
    val SDK_CORE_SOURCE_FILE_CONTENT =
        """
        package com.datadog.android.api

        open class SdkCore
        """.trimIndent()

    @Language("kotlin")
    val COMPONENT_PREDICATE_SOURCE_FILE_CONTENT =
        """
        package com.datadog.android.rum.tracking

        interface ComponentPredicate<T>
        """.trimIndent()

    @Language("kotlin")
    val ACCEPT_ALL_NAV_DESTINATIONS_SOURCE_FILE_CONTENT =
        """
        package com.datadog.android.rum.tracking

        import androidx.navigation.NavDestination

        class AcceptAllNavDestinations : ComponentPredicate<NavDestination>
        """.trimIndent()

    @Language("kotlin")
    val DATADOG_SOURCE_FILE_CONTENT =
        """
        package com.datadog.android

        import com.datadog.android.api.SdkCore

        object Datadog {
            fun getInstance(instanceName: String? = null): SdkCore = SdkCore()
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

    val sdkCoreSourceFile = SourceFile.kotlin(SDK_CORE_FILE_NAME, SDK_CORE_SOURCE_FILE_CONTENT)

    val componentPredicateSourceFile = SourceFile.kotlin(
        COMPONENT_PREDICATE_FILE_NAME,
        COMPONENT_PREDICATE_SOURCE_FILE_CONTENT
    )

    val acceptAllNavDestinationsSourceFile = SourceFile.kotlin(
        ACCEPT_ALL_NAV_DESTINATIONS_FILE_NAME,
        ACCEPT_ALL_NAV_DESTINATIONS_SOURCE_FILE_CONTENT
    )

    val datadogSourceFile = SourceFile.kotlin(DATADOG_FILE_NAME, DATADOG_SOURCE_FILE_CONTENT)

    // TODO RUM-8950: Dependency file should be separated in each file after the DSL configuration is introduced.
    val dependencyFiles = listOf(
        sdkCoreSourceFile,
        componentPredicateSourceFile,
        acceptAllNavDestinationsSourceFile,
        datadogSourceFile,
        trackingEffectSourceFile,
        datadogModifierSourceFile,
        composeInstrumentationAnnotationSourceFile
    )
}
