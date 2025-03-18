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
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@OptIn(ExperimentalCompilerApi::class)
class ComposeTagCompilationTest : KotlinCompilerTest() {

    @Mock
    private var mockCustomModifierCallback: () -> Unit = {}

    @Test
    fun `M not inject dd modifier W no modifier is present and plugin disabled`() {
        // Given
        val noModifierTestCaseSource = SourceFile.kotlin(
            NO_MODIFIER_TEST_CASE_FILE_NAME,
            NO_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = noModifierTestCaseSource,
            deps = dependencyFiles,
            enablePlugin = false
        )
        executeClassFile(
            classLoader = result.classLoader,
            className = "com.datadog.kcp.NoModifierTestCase",
            methodName = "NoModifierTestCase",
            methodArgs = listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verifyNoInteractions(mockCallback)
    }

    @Test
    fun `M inject dd modifier W no modifier is present and plugin enabled`() {
        // Given
        val noModifierTestCaseSource = SourceFile.kotlin(
            NO_MODIFIER_TEST_CASE_FILE_NAME,
            NO_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = noModifierTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            classLoader = result.classLoader,
            className = "com.datadog.kcp.NoModifierTestCase",
            methodName = "NoModifierTestCase",
            methodArgs = listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockCallback).invoke(false)
    }

    @Test
    fun `M not inject dd modifier W default modifier is present and plugin disabled`() {
        // Given
        val defaultModifierTestCaseSource = SourceFile.kotlin(
            DEFAULT_MODIFIER_TEST_CASE_FILE_NAME,
            DEFAULT_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = defaultModifierTestCaseSource,
            deps = dependencyFiles,
            enablePlugin = false
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.DefaultModifierTestCase",
            "DefaultModifierTestCase",
            listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verifyNoInteractions(mockCallback)
    }

    @Test
    fun `M inject dd modifier W default modifier is present and plugin enabled`() {
        // Given
        val defaultModifierTestCaseSource = SourceFile.kotlin(
            DEFAULT_MODIFIER_TEST_CASE_FILE_NAME,
            DEFAULT_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = defaultModifierTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.DefaultModifierTestCase",
            "DefaultModifierTestCase",
            listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockCallback).invoke(false)
    }

    @Test
    fun `M not inject dd modifier W custom modifier is present and plugin disabled`() {
        // Given
        val customModifierTestCaseSource = SourceFile.kotlin(
            CUSTOM_MODIFIER_TEST_CASE_FILE_NAME,
            CUSTOM_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = customModifierTestCaseSource,
            deps = dependencyFiles,
            enablePlugin = false
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.CustomModifierTestCase",
            "CustomModifierTestCase",
            listOf(mockCustomModifierCallback)
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verifyNoInteractions(mockCallback)
        verify(mockCustomModifierCallback).invoke()
    }

    @Test
    fun `M inject dd modifier W custom modifier is present`() {
        // Given
        val customModifierTestCaseSource = SourceFile.kotlin(
            CUSTOM_MODIFIER_TEST_CASE_FILE_NAME,
            CUSTOM_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = customModifierTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.CustomModifierTestCase",
            "CustomModifierTestCase",
            listOf(mockCustomModifierCallback)
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockCallback).invoke(false)
        verify(mockCustomModifierCallback).invoke()
    }

    @Test
    fun `M inject dd modifier with Image Role W Image is present`() {
        // Given
        val imageTestCaseSource = SourceFile.kotlin(
            IMAGE_TEST_CASE_FILE_NAME,
            IMAGE_TEST_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = imageTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.ImageTestCase",
            "ImageTestCase",
            listOf(mockCustomModifierCallback)
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockCallback).invoke(true)
        verify(mockCustomModifierCallback).invoke()
    }

    @Test
    fun `M inject dd modifier with Image Role W Icon is present`() {
        // Given
        val imageTestCaseSource = SourceFile.kotlin(
            ICON_TEST_CASE_FILE_NAME,
            ICON_TEST_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = imageTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.IconTestCase",
            "IconTestCase",
            listOf(mockCustomModifierCallback)
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockCallback).invoke(true)
        verify(mockCustomModifierCallback).invoke()
    }

    @Test
    fun `M inject dd modifier with Image Role W Async is present`() {
        // Given
        val imageTestCaseSource = SourceFile.kotlin(
            COIL_TEST_CASE_FILE_NAME,
            COIL_TEST_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(
            target = imageTestCaseSource,
            deps = dependencyFiles
        )
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.CoilTestCase",
            "CoilTestCase",
            listOf(mockCustomModifierCallback)
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockCallback).invoke(true)
        verify(mockCustomModifierCallback).invoke()
    }

    companion object {

        private const val NO_MODIFIER_TEST_CASE_FILE_NAME = "NoModifierTestCase.kt"
        private const val DEFAULT_MODIFIER_TEST_CASE_FILE_NAME = "DefaultModifierTestCase.kt"
        private const val CUSTOM_MODIFIER_TEST_CASE_FILE_NAME = "CustomModifierTestCase.kt"
        private const val IMAGE_TEST_CASE_FILE_NAME = "ImageTestCase.kt"
        private const val ICON_TEST_CASE_FILE_NAME = "IconTestCase.kt"
        private const val COIL_TEST_CASE_FILE_NAME = "CoilTestCase.kt"

        @Language("kotlin")
        private val NO_MODIFIER_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp
    
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            
            class NoModifierTestCase{
                @Composable
                fun NoModifierTestCase() {
                    CustomComposable(
                        text = "No Modifier Test Case"
                    )
                }
                
                @Composable
                fun CustomComposable(modifier : Modifier = Modifier, text: String){
                    // do nothing
                }

            }
            
            """.trimIndent()

        @Language("kotlin")
        private val DEFAULT_MODIFIER_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp
    
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            
            class DefaultModifierTestCase{
                @Composable
                fun DefaultModifierTestCase() {
                    CustomComposable(
                        modifier = Modifier,
                        text = "Default Modifier Test Case"
                    )
                }
                
                @Composable
                fun CustomComposable(modifier : Modifier = Modifier, text: String){
                    // do nothing
                }

            }
            
            """.trimIndent()

        @Language("kotlin")
        private val CUSTOM_MODIFIER_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp
    
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            
            class CustomModifierTestCase{
                @Composable
                fun CustomModifierTestCase(customCallback : () -> Unit) {
                    CustomComposable(
                        modifier = Modifier.stubModifier(customCallback),
                        text = "Custom Modifier Test Case"
                    )
                }
                
                @Composable
                fun CustomComposable(modifier : Modifier = Modifier, text: String){
                    // do nothing
                }

                @Composable
                fun Modifier.stubModifier(customCallback: () -> Unit): Modifier{
                    customCallback.invoke()
                    return Modifier
                }

            }
            """.trimIndent()

        @Language("kotlin")
        private val IMAGE_TEST_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp
    
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.Image
            import androidx.compose.ui.Modifier
            
            class ImageTestCase{
                @Composable
                fun ImageTestCase(customCallback : () -> Unit) {
                    Image(
                        modifier = Modifier.stubModifier(customCallback)
                    )
                }
                
                @Composable
                fun Modifier.stubModifier(customCallback: () -> Unit): Modifier{
                    customCallback.invoke()
                    return Modifier
                }

            }
            """.trimIndent()

        @Language("kotlin")
        private val ICON_TEST_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp
    
            import androidx.compose.runtime.Composable
            import androidx.compose.material.Icon
            import androidx.compose.ui.Modifier
            
            class IconTestCase{
                @Composable
                fun IconTestCase(customCallback : () -> Unit) {
                    Icon(
                        modifier = Modifier.stubModifier(customCallback)
                    )
                }
                
                @Composable
                fun Modifier.stubModifier(customCallback: () -> Unit): Modifier{
                    customCallback.invoke()
                    return Modifier
                }

            }
            """.trimIndent()
    }

    @Language("kotlin")
    private val COIL_TEST_SOURCE_FILE_CONTENT =
        """
            package com.datadog.kcp
    
            import androidx.compose.runtime.Composable
            import coil.compose.AsyncImage
            import androidx.compose.ui.Modifier
            
            class CoilTestCase{
                @Composable
                fun CoilTestCase(customCallback : () -> Unit) {
                    AsyncImage(
                        modifier = Modifier.stubModifier(customCallback)
                    )
                }
                
                @Composable
                fun Modifier.stubModifier(customCallback: () -> Unit): Modifier{
                    customCallback.invoke()
                    return Modifier
                }

            }
        """.trimIndent()
}
