package com.datadog.gradle.plugin.kcp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@OptIn(ExperimentalCompilerApi::class)
class ComposeNavHostCompilationTest : KotlinCompilerTest() {

    @Test
    fun `M inject 'NavigationViewTrackingEffect' W found nav host`() {
        // Given
        val navHostTestCaseSource = SourceFile.kotlin(
            NAV_HOST_TEST_CASE_FILE_NAME,
            NAV_HOST_TEST_CASE_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = navHostTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.android.instrumented.NavHostTestCase",
            "NavHostTestCase",
            emptyList()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        // TODO RUM-8951: should be changed to 1 interaction after implementing the transformer for NavHost
        verifyNoInteractions(mockCallback)
    }

    @Test
    fun `M inject 'NavigationViewTrackingEffect' W found nav host with expression`() {
        // Given
        val navHostTestCaseSource = SourceFile.kotlin(
            NAV_HOST_TEST_CASE_FILE_NAME,
            NAV_HOST_TEST_CASE_WITH_EXPRESSION_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = navHostTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.android.instrumented.NavHostTestCase",
            "NavHostTestCase",
            emptyList()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        // TODO RUM-8951: should be changed to 1 interaction after implementing the transformer for NavHost
        verifyNoInteractions(mockCallback)
    }

    companion object {

        private const val NAV_HOST_TEST_CASE_FILE_NAME = "NavHostTestCase.kt"

        @Language("kotlin")
        private val NAV_HOST_TEST_CASE_SOURCE_FILE_CONTENT =
            """
            package com.datadog.android.instrumented

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
        private val NAV_HOST_TEST_CASE_WITH_EXPRESSION_SOURCE_FILE_CONTENT =
            """
            package com.datadog.android.instrumented

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
}
