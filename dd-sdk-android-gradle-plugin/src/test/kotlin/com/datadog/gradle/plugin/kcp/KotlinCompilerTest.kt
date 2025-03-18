package com.datadog.gradle.plugin.kcp

import com.datadog.gradle.plugin.InstrumentationMode
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

    // TODO RUM-8950: Dependency file should be separated in each file after the DSL configuration is introduced.
    protected val dependencyFiles = listOf(
        trackingEffectSourceFileContent,
        datadogModifierSourceFileContent
    )

    protected fun compileFile(
        target: SourceFile,
        deps: List<SourceFile>,
        enablePlugin: Boolean = true
    ): KotlinCompilation.Result {
        val pluginRegistrars = if (enablePlugin) {
            listOf(
                DatadogPluginRegistrar(
                    InternalCompilerConfiguration(
                        trackViews = InstrumentationMode.AUTO,
                        recordImages = InstrumentationMode.AUTO
                    )
                )
            )
        } else {
            listOf()
        }

        return KotlinCompilation().apply {
            sources = deps + target
            componentRegistrars = pluginRegistrars
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
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

        @Language("kotlin")
        private val DD_MODIFIER_SOURCE_FILE_CONTENT =
            """
            package com.datadog.kcp.compose
            
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.semantics.SemanticsPropertyKey
            import androidx.compose.ui.semantics.SemanticsPropertyReceiver
            import androidx.compose.ui.semantics.semantics
            import com.datadog.gradle.plugin.kcp.TestCallbackContainer
            
            fun Modifier.datadog(name: String, isImageRole: Boolean = false): Modifier {
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
            fun NavigationViewTrackingEffect(
                navController: NavController
            ) {
                TestCallbackContainer.invokeCallback()
            }
            """.trimIndent()
    }
}
