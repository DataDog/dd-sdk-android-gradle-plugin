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
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.reflect.full.declaredFunctions

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@OptIn(ExperimentalCompilerApi::class)
abstract class KotlinCompilerTest {

    @Mock
    protected lateinit var mockCallback: (Boolean) -> Unit

    abstract fun registerDatadogPluginRegistrar(
        compilation: KotlinCompilation,
        instrumentationMode: InstrumentationMode
    )

    // TODO RUM-8950: Dependency file should be separated in each file after the DSL configuration is introduced.
    protected val dependencyFiles = KotlinCompilerTestSources.dependencyFiles

    protected fun compileFile(
        target: SourceFile,
        deps: List<SourceFile>,
        enablePlugin: Boolean = true,
        instrumentationMode: InstrumentationMode = InstrumentationMode.AUTO
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = deps + target
            if (enablePlugin) {
                registerDatadogPluginRegistrar(this, instrumentationMode)
            }
            inheritClassPath = true
            messageOutputStream = System.out
            // Kotlin 2.4.0 made CommonCompilerArguments.setOptIn non-null; kctfork
            // defaults this property to null, which triggers a NPE in the setter.
            optIn = emptyList()
            // The project modules are compiled with a newer Kotlin version than the embedded
            // compiler used in kotlin20/21/22 test modules. Skip the metadata version check
            // so the embedded compiler can read classes compiled with a newer metadata format.
            kotlincArguments += "-Xskip-metadata-version-check"
        }.compile()
    }

    @Suppress("SpreadOperator")
    protected fun executeClassFile(
        classLoader: ClassLoader,
        className: String,
        methodName: String,
        methodArgs: List<Any> = emptyList()
    ) {
        val deps = listOf(
            KotlinCompilerTestSources.NAV_HOST_BUILDER_PATH,
            KotlinCompilerTestSources.NAV_GRAPH_BUILDER_PATH,
            KotlinCompilerTestSources.NAV_HOST_CONTROLLER_PATH
        )

        deps.forEach {
            classLoader.loadClass(it)
        }

        // Setup the TestCallbackContainer and the mock callback.
        val testCallbackContainerClazz = classLoader.loadClass(KotlinCompilerTestSources.TEST_CALLBACK_CONTAINER_PATH)
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
}
