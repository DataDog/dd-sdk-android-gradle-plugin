package com.datadog.android.instrumented

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.test.platform.app.InstrumentationRegistry
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `M call function and have original components W instrument with the Plugin`() {
        // Given
        val latch = CountDownLatch(1)
        var isFunctionCalled = false

        // When
        val lifecycleOwner = composeTestRule.setContentWithStubLifeCycle {
            ScreenWithNavHost(onEvent = { navHost, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val listeners = getOnDestinationChangedListenerSize(navHost)
                    if (listeners > 0) {
                        isFunctionCalled = true
                    }
                    latch.countDown()
                }
            })
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            lifecycleOwner.setCurrentState(Lifecycle.State.RESUMED)
        }

        // Then
        composeTestRule
            .onNodeWithText(TEST_TEXT)
            .assertIsDisplayed()

        latch.await(5, TimeUnit.SECONDS)
        assertThat(isFunctionCalled).isTrue()
    }

    @Test
    fun `M call function and have original components W instrument nested NavHost with the Plugin`() {
        // Given
        val latch = CountDownLatch(1)
        var isFunctionCalled = false

        // When
        val lifecycleOwner = composeTestRule.setContentWithStubLifeCycle {
            ScreenWithNavHostNested(onEvent = { navHost, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val listeners = getOnDestinationChangedListenerSize(navHost)
                    if (listeners > 0) {
                        isFunctionCalled = true
                    }
                    latch.countDown()
                }
            })
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            lifecycleOwner.setCurrentState(Lifecycle.State.RESUMED)
        }

        // Then
        composeTestRule
            .onNodeWithText(TEST_TEXT)
            .assertIsDisplayed()

        latch.await(5, TimeUnit.SECONDS)
        assertThat(isFunctionCalled).isTrue()
    }

    private fun getOnDestinationChangedListenerSize(navHostController: NavHostController): Int {
        val listenersField = NavController::class.java.getDeclaredField("onDestinationChangedListeners")
        listenersField.isAccessible = true
        val listeners = listenersField.get(navHostController) as Collection<*>
        return listeners.size
    }

    private fun ComposeContentTestRule.setContentWithStubLifeCycle(
        composable: @Composable () -> Unit
    ): StubLifecycleOwner {
        var stubLifecycleOwner: StubLifecycleOwner? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            stubLifecycleOwner = StubLifecycleOwner()
        }
        this.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides stubLifecycleOwner!!
            ) {
                composable()
            }
        }
        return stubLifecycleOwner!!
    }

    class StubLifecycleOwner : LifecycleOwner {

        private val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle
            get() = registry

        init {
            registry.currentState = Lifecycle.State.STARTED
        }

        fun setCurrentState(state: Lifecycle.State) {
            registry.currentState = state
        }
    }
}
