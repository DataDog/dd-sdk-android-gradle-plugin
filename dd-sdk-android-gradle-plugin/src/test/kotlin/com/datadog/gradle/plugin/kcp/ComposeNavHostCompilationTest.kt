/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2020-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@Extensions(ExtendWith(ForgeExtension::class))
@OptIn(ExperimentalCompilerApi::class)
abstract class ComposeNavHostCompilationTest : KotlinCompilerTest() {

    @Test
    fun `M inject 'NavigationViewTrackingEffect' W found nav host`(
        @Forgery fakeInstrumentationMode: InstrumentationMode
    ) {
        // Given
        val navHostTestCaseSource = SourceFile.kotlin(
            NAV_HOST_TEST_CASE_FILE_NAME,
            NAV_HOST_TEST_CASE_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = navHostTestCaseSource,
            deps = dependencyFiles,
            internalInstrumentationMode = fakeInstrumentationMode
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.NavHostTestCase",
            "NavHostTestCase",
            emptyList()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        if (fakeInstrumentationMode == InstrumentationMode.AUTO) {
            verify(mockCallback).invoke(false)
        } else {
            verifyNoInteractions(mockCallback)
        }
    }

    @Test
    fun `M inject 'NavigationViewTrackingEffect' W found nav host with apply expression`(
        @Forgery instrumentationMode: InstrumentationMode
    ) {
        // Given
        val navHostTestCaseSource = SourceFile.kotlin(
            NAV_HOST_TEST_CASE_FILE_NAME,
            NAV_HOST_TEST_CASE_WITH_APPLY_EXPRESSION_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = navHostTestCaseSource,
            deps = dependencyFiles,
            internalInstrumentationMode = instrumentationMode
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.NavHostTestCase",
            "NavHostTestCase",
            emptyList()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        if (instrumentationMode == InstrumentationMode.AUTO) {
            verify(mockCallback).invoke(false)
        } else {
            verifyNoInteractions(mockCallback)
        }
    }

    @Test
    fun `M inject 'NavigationViewTrackingEffect' W found nav host with rememberNavController call`(
        @Forgery instrumentationMode: InstrumentationMode
    ) {
        // Given
        val navHostTestCaseSource = SourceFile.kotlin(
            NAV_HOST_TEST_CASE_FILE_NAME,
            NAV_HOST_TEST_CASE_WITH_CALL_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = navHostTestCaseSource,
            deps = dependencyFiles,
            internalInstrumentationMode = instrumentationMode
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.NavHostTestCase",
            "NavHostTestCase",
            emptyList()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        if (instrumentationMode == InstrumentationMode.AUTO) {
            verify(mockCallback).invoke(false)
        } else {
            verifyNoInteractions(mockCallback)
        }
    }

    @Test
    fun `M match given instrumentation mode W instrument annotated function`(
        @Forgery instrumentationMode: InstrumentationMode
    ) {
        // Given
        val navHostTestCaseSource = SourceFile.kotlin(
            NAV_HOST_TEST_CASE_FILE_NAME,
            NAV_HOST_TEST_CASE_WITH_ANNOTATION_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = navHostTestCaseSource,
            deps = dependencyFiles,
            internalInstrumentationMode = instrumentationMode
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.NavHostTestCase",
            "NavHostTestCase",
            emptyList()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        if (instrumentationMode != InstrumentationMode.DISABLE) {
            verify(mockCallback).invoke(false)
        } else {
            verifyNoInteractions(mockCallback)
        }
    }

    @Test
    fun `M instrument NavHost W NavHost is nested`(
        @Forgery instrumentationMode: InstrumentationMode
    ) {
        // Given
        val navHostTestCaseSource = SourceFile.kotlin(
            NAV_HOST_TEST_CASE_FILE_NAME,
            NAV_HOST_NESTED_TEST_CASE_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = navHostTestCaseSource,
            deps = dependencyFiles,
            internalInstrumentationMode = instrumentationMode
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.NavHostNestedTestCase",
            "NavHostNestedTestCase",
            emptyList()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        if (instrumentationMode != InstrumentationMode.DISABLE) {
            verify(mockCallback).invoke(false)
        } else {
            verifyNoInteractions(mockCallback)
        }
    }

    companion object {

        private const val NAV_HOST_TEST_CASE_FILE_NAME = "NavHostTestCase.kt"

        @Language("kotlin")
        private val NAV_HOST_TEST_CASE_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp

            import androidx.compose.runtime.Composable
            import androidx.navigation.compose.NavHost
            import androidx.navigation.compose.composable
            import androidx.navigation.compose.rememberNavController
            
            class NavHostTestCase{
                @Composable
                internal fun NavHostTestCase() {
                    val navHost = rememberNavController()       
                    NavHost(navHost,""){
                
                    }          
                }
            }
            """.trimIndent()

        @Language("kotlin")
        private val NAV_HOST_TEST_CASE_WITH_APPLY_EXPRESSION_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp

            import androidx.compose.runtime.Composable
            import androidx.navigation.compose.NavHost
            import androidx.navigation.compose.composable
            import androidx.navigation.compose.rememberNavController
            
            class NavHostTestCase{
                @Composable
                internal fun NavHostTestCase() {
                    val navHost = rememberNavController()       
                    NavHost(navHost.apply{
                        // do nothing
                     }
                    ,""){
                
                    }          
                }
            }
            """.trimIndent()
    }

    @Language("kotlin")
    private val NAV_HOST_TEST_CASE_WITH_CALL_SOURCE_FILE_CONTENT =
        """
            package com.datadog.kcp

            import androidx.compose.runtime.Composable
            import androidx.navigation.compose.NavHost
            import androidx.navigation.compose.composable
            import androidx.navigation.compose.rememberNavController
            
            class NavHostTestCase{
                @Composable
                internal fun NavHostTestCase() {
                    NavHost(rememberNavController(),""){
                
                    }          
                }
            }
        """.trimIndent()

    @Language("kotlin")
    private val NAV_HOST_TEST_CASE_WITH_ANNOTATION_SOURCE_FILE_CONTENT =
        """
            package com.datadog.kcp

            import androidx.compose.runtime.Composable
            import androidx.navigation.compose.NavHost
            import androidx.navigation.compose.composable
            import androidx.navigation.compose.rememberNavController
            import com.datadog.android.compose.ComposeInstrumentation
            
            class NavHostTestCase{
                @ComposeInstrumentation
                @Composable
                internal fun NavHostTestCase() {
                    val navHost = rememberNavController()       
                    NavHost(navHost,""){
                
                    }          
                }
            }
        """.trimIndent()

    @Language("kotlin")
    private val NAV_HOST_NESTED_TEST_CASE_SOURCE_FILE_CONTENT =
        """
            package com.datadog.kcp

            import androidx.compose.runtime.Composable
            import androidx.navigation.compose.NavHost
            import androidx.navigation.compose.composable
            import androidx.navigation.compose.rememberNavController
            import com.datadog.android.compose.ComposeInstrumentation
            
            class NavHostNestedTestCase{
                @ComposeInstrumentation
                @Composable
                internal fun NavHostNestedTestCase() {
                
                    Container{
                        val navHost = rememberNavController()       
                        NavHost(navHost,""){
                    
                        }  
                    }               
                           
                }
    
                @Composable
                fun Container(content: @Composable () -> Unit){
                    content.invoke()
                }

            }
        """.trimIndent()
}
