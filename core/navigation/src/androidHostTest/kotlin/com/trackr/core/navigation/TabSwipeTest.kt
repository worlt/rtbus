package com.trackr.core.navigation

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TabSwipeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var nav: Navigator
    private lateinit var state: NavigationState
    private val lifecycles = mutableMapOf<NavKey, Lifecycle>()
    private val mounts = mutableMapOf<NavKey, Int>()
    private val sheetVisible = mutableStateOf(false)
    private var dialogBack: OnBackPressedDispatcher? = null
    private val disposals = mutableMapOf<NavKey, Int>()

    @OptIn(ExperimentalMaterial3Api::class)
    private fun launch() {
        rule.setContent {
            state = rememberNavigationState(topLevel = TopLevel.visibleTabs())
            nav = remember(state) { Navigator(state) }
            TabNavigationDisplay(state, { key -> NavEntry<NavKey>(key) {
                lifecycles[key] = LocalLifecycleOwner.current.lifecycle
                DisposableEffect(key) {
                    mounts[key] = (mounts[key] ?: 0) + 1
                    onDispose { disposals[key] = (disposals[key] ?: 0) + 1 }
                }
                Box(Modifier.fillMaxSize().background(Color.White))
                if (key == ScreenKey.More && sheetVisible.value) {
                    val sheet = rememberBottomSheetState(
                        initialValue = SheetValue.Hidden,
                        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                    )
                    ModalBottomSheet(onDismissRequest = { sheetVisible.value = false }, sheetState = sheet) {
                        val view = LocalView.current
                        SideEffect { dialogBack = view.findViewTreeOnBackPressedDispatcherOwner()?.onBackPressedDispatcher }
                        Box(Modifier.size(100.dp).testTag("modal"))
                    }
                }
            } }, { nav.back() }, Modifier.size(320.dp, 600.dp).testTag("tabs"), onSelectTab = nav::selectTab)
        }
        rule.waitForIdle()
    }

    @Test fun horizontalSwipeChangesTabsInBothDirections() {
        launch()
        rule.onNodeWithTag("tabs").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(ScreenKey.More, state.topLevel) }
        rule.onNodeWithTag("tabs").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(ScreenKey.Home, state.topLevel) }
    }

    @Test fun revisitingTabsRetainsTheirCompositions() {
        launch()
        repeat(3) {
            rule.runOnIdle { nav.selectTab(ScreenKey.More) }
            rule.waitForIdle()
            rule.runOnIdle { nav.selectTab(ScreenKey.Home) }
            rule.waitForIdle()
        }
        rule.runOnIdle {
            assertEquals(1, mounts[ScreenKey.Home])
            assertEquals(1, mounts[ScreenKey.More])
            assertEquals(0, disposals.values.sum())
        }
    }

    @Test fun inactiveRootIsStoppedWhileItsCompositionIsRetained() {
        launch()
        rule.runOnIdle {
            assertEquals(Lifecycle.State.RESUMED, lifecycles[ScreenKey.Home]?.currentState)
            assertEquals(Lifecycle.State.CREATED, lifecycles[ScreenKey.More]?.currentState)
            nav.selectTab(ScreenKey.More)
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(Lifecycle.State.CREATED, lifecycles[ScreenKey.Home]?.currentState)
            assertEquals(Lifecycle.State.RESUMED, lifecycles[ScreenKey.More]?.currentState)
            assertEquals(0, disposals.values.sum())
        }
    }

    @Test fun verticalGestureDoesNotChangeTabs() {
        launch()
        rule.onNodeWithTag("tabs").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(ScreenKey.Home, state.topLevel) }
    }

    @Test fun modalBackDismissesTheDialogBeforeSwitchingTabs() {
        launch()
        rule.runOnIdle { nav.selectTab(ScreenKey.More) }
        rule.waitForIdle()
        rule.runOnIdle { sheetVisible.value = true }
        rule.waitForIdle()
        rule.runOnIdle { requireNotNull(dialogBack).onBackPressed() }
        rule.waitForIdle()
        rule.runOnIdle {
            assertFalse("dialog window must receive its own back event", sheetVisible.value)
            assertEquals(ScreenKey.More, state.topLevel)
        }
    }

    @Test fun childPageDoesNotSwipeToAnotherTab() {
        launch()
        rule.runOnIdle { nav.open(ScreenKey.Search) }
        rule.waitForIdle()
        rule.onNodeWithTag("tabs").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(ScreenKey.Home, state.topLevel)
            assertEquals(ScreenKey.Search, state.currentKey)
        }
    }
}
