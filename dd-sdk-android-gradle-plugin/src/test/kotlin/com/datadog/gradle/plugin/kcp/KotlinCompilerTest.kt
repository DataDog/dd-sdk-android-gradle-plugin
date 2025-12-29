/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import kotlin.reflect.full.declaredFunctions

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@OptIn(ExperimentalCompilerApi::class)
open class KotlinCompilerTest {

    @Mock
    protected lateinit var mockCallback: (Boolean) -> Unit

    private val trackingEffectSourceFileContent = SourceFile.kotlin(
        TRACKING_EFFECT_FILE_NAME,
        TRACKING_EFFECT_SOURCE_FILE_CONTENT
    )

    private val datadogModifierSourceFileContent = SourceFile.kotlin(
        DD_MODIFIER_CLASS_FILE_NAME,
        DD_MODIFIER_SOURCE_FILE_CONTENT
    )

    private val composeInstrumentationAnnotationSourceFileContent = SourceFile.kotlin(
        COMPOSE_INSTRUMENTATION_ANNOTATION_FILE_NAME,
        COMPOSE_INSTRUMENTATION_ANNOTATION_SOURCE_FILE_CONTENT
    )

    // TODO RUM-8950: Dependency file should be separated in each file after the DSL configuration is introduced.
    protected val dependencyFiles = listOf(
        trackingEffectSourceFileContent,
        datadogModifierSourceFileContent,
        composeInstrumentationAnnotationSourceFileContent
    )

    protected fun compileFile(
        target: SourceFile,
        deps: List<SourceFile>,
        enablePlugin: Boolean = true,
        internalInstrumentationMode: InstrumentationMode = InstrumentationMode.AUTO
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = deps + target
            configurePluginRegistrars(enablePlugin, internalInstrumentationMode)
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
    }

    /**
     * Configures plugin registrars using either the new API (compilerPluginRegistrars)
     * or the old API (componentRegistrars) depending on what's available.
     * kctfork 0.12.0+ uses compilerPluginRegistrars, older versions use componentRegistrars.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun KotlinCompilation.configurePluginRegistrars(
        enablePlugin: Boolean,
        internalInstrumentationMode: InstrumentationMode
    ) {
        if (!enablePlugin) return

        // Try the new API first (compilerPluginRegistrars) - available in kctfork 0.12.0+
        try {
            val newApiProperty = this::class.java.methods.find { it.name == "setCompilerPluginRegistrars" }
            if (newApiProperty != null) {
                val registrars = listOf(DatadogCompilerPluginRegistrar(internalInstrumentationMode))
                newApiProperty.invoke(this, registrars)
                return
            }
        } catch (_: Exception) {
            // Fall through to old API
        }

        // Fall back to old API (componentRegistrars) - available in kctfork 0.8.0 and earlier
        try {
            @Suppress("DEPRECATION")
            componentRegistrars = listOf(DatadogPluginRegistrar(internalInstrumentationMode))
        } catch (e: Exception) {
            throw IllegalStateException(
                "Unable to configure plugin registrars. " +
                    "Neither compilerPluginRegistrars nor componentRegistrars API is available.",
                e
            )
        }
    }

    protected fun executeClassFile(
        classLoader: ClassLoader,
        className: String,
        methodName: String,
        methodArgs: List<Any> = emptyList()
    ) {
        val deps = listOf(
            NAV_HOST_BUILDER_PATH,
            NAV_GRAPH_BUILDER_PATH,
            NAV_HOST_CONTROLLER_PATH
        )

        deps.forEach {
            classLoader.loadClass(it)
        }

        // Setup the TestCallbackContainer and the mock callback.
        val testCallbackContainerClazz = classLoader.loadClass(TEST_CALLBACK_CONTAINER_PATH)
        val setCallbackFunc = testCallbackContainerClazz.kotlin.declaredFunctions.find { it.name == "setCallback" }
        val testCallbackContainerInstance = testCallbackContainerClazz.getField("INSTANCE").get(null)
        setCallbackFunc?.call(testCallbackContainerInstance, mockCallback)

        // Load the target class.
        val clazz = classLoader.loadClass(className)
        val instance = clazz.getDeclaredConstructor().newInstance()
        val method = clazz.kotlin.declaredFunctions.find { it.name == methodName }

        // Call the function.
        val args = methodArgs.toTypedArray()
        method?.call(instance, *args)
    }

    companion object {

        private const val TEST_CALLBACK_CONTAINER_PATH = "com.datadog.gradle.plugin.kcp.TestCallbackContainer"
        private const val NAV_GRAPH_BUILDER_PATH = "androidx.navigation.compose.NavGraphBuilder"
        private const val NAV_HOST_BUILDER_PATH = "androidx.navigation.compose.NavHost"
        private const val NAV_HOST_CONTROLLER_PATH = "androidx.navigation.NavHostController"
        private const val TRACKING_EFFECT_FILE_NAME = "Navigation.kt"
        private const val DD_MODIFIER_CLASS_FILE_NAME = "DatadogModifier.kt"
        private const val COMPOSE_INSTRUMENTATION_ANNOTATION_FILE_NAME = "ComposeInstrumentation.kt"

        @Language("kotlin")
        private val DD_MODIFIER_SOURCE_FILE_CONTENT =
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
        private val TRACKING_EFFECT_SOURCE_FILE_CONTENT =
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
        private val COMPOSE_INSTRUMENTATION_ANNOTATION_SOURCE_FILE_CONTENT =
            """
            package com.datadog.android.compose

            @Retention(AnnotationRetention.BINARY)
            annotation class ComposeInstrumentation

            """.trimIndent()
    }
}
