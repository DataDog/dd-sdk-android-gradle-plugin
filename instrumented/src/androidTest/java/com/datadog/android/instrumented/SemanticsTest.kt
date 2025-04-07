package com.datadog.android.instrumented

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
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
    fun `M have datadog semantics tag W modifier is absent`() {
        composeTestRule.setContent {
            ScreenWithoutModifier()
        }
        val columnSemanticsMatcher = hasSemanticsValue(DD_SEMANTICS_KEY_NAME, "Column")
        composeTestRule.onAllNodes(columnSemanticsMatcher).assertCountEquals(1)
        val textSemanticsMatcher = hasSemanticsValue(DD_SEMANTICS_KEY_NAME, "Text")
        composeTestRule.onAllNodes(textSemanticsMatcher).assertCountEquals(1)
    }

    @Test
    fun `M have datadog semantics tag W modifier is default`() {
        composeTestRule.setContent {
            ScreenWithDefaultModifier()
        }
        val textSemanticsMatcher = hasSemanticsValue(DD_SEMANTICS_KEY_NAME, "Text")
        composeTestRule.onAllNodes(textSemanticsMatcher).assertCountEquals(1)
        val columnSemanticsMatcher = hasSemanticsValue(DD_SEMANTICS_KEY_NAME, "Column")
        composeTestRule.onAllNodes(columnSemanticsMatcher).assertCountEquals(1)
    }

    @Test
    fun `M have datadog semantics tag W modifier is custom`() {
        composeTestRule.setContent {
            ScreenWithCustomModifier()
        }
        val textSemanticsMatcher = hasSemanticsValue(DD_SEMANTICS_KEY_NAME, "Text")
        composeTestRule.onAllNodes(textSemanticsMatcher).assertCountEquals(1)
        val columnSemanticsMatcher = hasSemanticsValue(DD_SEMANTICS_KEY_NAME, "Column")
        composeTestRule.onAllNodes(columnSemanticsMatcher).assertCountEquals(1)
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
    }
}
