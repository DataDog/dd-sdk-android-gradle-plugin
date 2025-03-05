package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.kcp.DatadogPluginRegistrar
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import kotlin.reflect.full.declaredFunctions

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@OptIn(ExperimentalCompilerApi::class)
class KotlinCompilerTest {

    private val datadogModifierSourceFileContent = SourceFile.kotlin(
        DD_MODIFIER_CLASS_FILE_NAME,
        DD_MODIFIER_SOURCE_FILE_CONTENT
    )

    @Mock
    private lateinit var mockDataDogModifierCallback: () -> Unit

    @Mock
    private lateinit var mockCustomModifierCallback: () -> Unit

    @BeforeEach
    fun `set up`() {
        whenever(mockDataDogModifierCallback.invoke()) doReturn Unit
        whenever(mockCustomModifierCallback.invoke()) doReturn Unit
    }

    @Test
    fun `M not inject dd modifier W no modifier is present and plugin disabled`() {
        // Given
        val noModifierTestCaseSource = SourceFile.kotlin(
            NO_MODIFIER_TEST_CASE_FILE_NAME,
            NO_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(noModifierTestCaseSource, false)
        executeClassFile(
            classLoader = result.classLoader,
            className = "com.datadog.kcp.NoModifierTestCase",
            methodName = "NoModifierTestCase",
            methodArgs = listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verifyNoInteractions(mockDataDogModifierCallback)
    }

    @Test
    fun `M inject dd modifier W no modifier is present and plugin enabled`() {
        // Given
        val noModifierTestCaseSource = SourceFile.kotlin(
            NO_MODIFIER_TEST_CASE_FILE_NAME,
            NO_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(noModifierTestCaseSource)
        executeClassFile(
            classLoader = result.classLoader,
            className = "com.datadog.kcp.NoModifierTestCase",
            methodName = "NoModifierTestCase",
            methodArgs = listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockDataDogModifierCallback).invoke()
    }

    @Test
    fun `M not inject dd modifier W default modifier is present and plugin disabled`() {
        // Given
        val defaultModifierTestCaseSource = SourceFile.kotlin(
            DEFAULT_MODIFIER_TEST_CASE_FILE_NAME,
            DEFAULT_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(defaultModifierTestCaseSource, false)
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.DefaultModifierTestCase",
            "DefaultModifierTestCase",
            listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verifyNoInteractions(mockDataDogModifierCallback)
    }

    @Test
    fun `M inject dd modifier W default modifier is present and plugin enabled`() {
        // Given
        val defaultModifierTestCaseSource = SourceFile.kotlin(
            DEFAULT_MODIFIER_TEST_CASE_FILE_NAME,
            DEFAULT_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(defaultModifierTestCaseSource)
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.DefaultModifierTestCase",
            "DefaultModifierTestCase",
            listOf()
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockDataDogModifierCallback).invoke()
    }

    @Test
    fun `M not inject dd modifier W custom modifier is present and plugin disabled`() {
        // Given
        val customModifierTestCaseSource = SourceFile.kotlin(
            CUSTOM_MODIFIER_TEST_CASE_FILE_NAME,
            CUSTOM_MODIFIER_SOURCE_FILE_CONTENT
        )

        // When
        val result = compileFile(customModifierTestCaseSource, false)
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.CustomModifierTestCase",
            "CustomModifierTestCase",
            listOf(mockCustomModifierCallback)
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verifyNoInteractions(mockDataDogModifierCallback)
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
        val result = compileFile(customModifierTestCaseSource)
        executeClassFile(
            result.classLoader,
            "com.datadog.kcp.CustomModifierTestCase",
            "CustomModifierTestCase",
            listOf(mockCustomModifierCallback)
        )

        // Then
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        verify(mockDataDogModifierCallback).invoke()
        verify(mockCustomModifierCallback).invoke()
    }

    private fun compileFile(
        file: SourceFile,
        enablePlugin: Boolean = true
    ): KotlinCompilation.Result {
        val pluginRegistrars = if (enablePlugin) {
            listOf(DatadogPluginRegistrar())
        } else {
            listOf()
        }

        return KotlinCompilation().apply {
            sources = listOf(datadogModifierSourceFileContent, file)
            compilerPluginRegistrars = pluginRegistrars
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
    }

    private fun executeClassFile(
        classLoader: ClassLoader,
        className: String,
        methodName: String,
        methodArgs: List<Any> = emptyList()
    ) {
        // Setup the TestCallbackContainer and the mock callback.
        val testCallbackContainerClazz = classLoader.loadClass(TEST_CALLBACK_CONTAINER_PATH)
        val setCallbackFunc = testCallbackContainerClazz.kotlin.declaredFunctions.find { it.name == "setCallback" }
        val testCallbackContainerInstance = testCallbackContainerClazz.getField("INSTANCE").get(null)
        setCallbackFunc?.call(testCallbackContainerInstance, mockDataDogModifierCallback)

        // Load the target class.
        val clazz = classLoader.loadClass(className)
        val instance = clazz.getDeclaredConstructor().newInstance()
        val method = clazz.kotlin.declaredFunctions.find { it.name == methodName }

        // Call the function.
        val args = methodArgs.toTypedArray()
        method?.call(instance, *args)
    }

    companion object {

        private const val DD_MODIFIER_CLASS_FILE_NAME = "DatadogModifier.kt"
        private const val NO_MODIFIER_TEST_CASE_FILE_NAME = "NoModifierTestCase.kt"
        private const val DEFAULT_MODIFIER_TEST_CASE_FILE_NAME = "DefaultModifierTestCase.kt"
        private const val CUSTOM_MODIFIER_TEST_CASE_FILE_NAME = "CustomModifierTestCase.kt"
        private const val TEST_CALLBACK_CONTAINER_PATH = "com.datadog.gradle.plugin.kcp.TestCallbackContainer"

        @Language("kotlin")
        private val DD_MODIFIER_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp.compose
            
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.semantics.SemanticsPropertyKey
            import androidx.compose.ui.semantics.SemanticsPropertyReceiver
            import androidx.compose.ui.semantics.semantics
            import com.datadog.gradle.plugin.kcp.TestCallbackContainer
            
            fun Modifier.datadog(name: String): Modifier {
                TestCallbackContainer.invokeCallback()
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
    }
}
