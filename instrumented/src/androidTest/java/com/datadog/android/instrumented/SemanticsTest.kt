package com.datadog.android.instrumented

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
class SemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `M have no datadog semantics tag W compose kotlin compiler plugin is not registered`() {
        composeTestRule.setContent {
            TestScreen()
        }
        val semanticsMatcher = hasSemanticsValue(DD_SEMANTICS_KEY_NAME, DD_SEMANTICS_VALUE_DEFAULT)
        composeTestRule.onNode(semanticsMatcher).assertDoesNotExist()
    }

    private fun <T> hasSemanticsValue(
        keyName: String,
        expectedValue: T
    ): SemanticsMatcher {
        return SemanticsMatcher("$keyName = $expectedValue") { node ->
            val entry = node.config.firstOrNull { it.key.name == keyName }
            entry != null && entry.value == expectedValue
        }
    }

    companion object {
        private const val DD_SEMANTICS_KEY_NAME = "_dd_semantics"
        private const val DD_SEMANTICS_VALUE_DEFAULT = "DD_DEFAULT_TAG"
    }
}
