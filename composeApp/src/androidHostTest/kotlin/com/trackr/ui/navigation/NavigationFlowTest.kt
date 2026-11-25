package com.trackr.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trackr.core.model.DataSourceType
import com.trackr.core.navigation.NavigationState
import com.trackr.core.navigation.Navigator
import com.trackr.core.navigation.ScreenKey
import com.trackr.core.navigation.TopLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * RTBus 导航边界：规划入口不再显示，但普通路线详情仍可作为当前页签栈的子页保留。
 * 旧版 RoutePlan Key 只用于兼容恢复，不应重新成为可见页签。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val line = ScreenKey.LineDetail(
        cityId = "021",
        lineId = "line_49",
        targetOrder = 4,
        stopId = "stop_004",
        source = DataSourceType.CHELAILE,
        lineName = "49路",
    )

    private fun newState() = NavigationState(
        start = TopLevel.start,
        current = mutableStateOf(TopLevel.start),
        stacks = TopLevel.tabs.associateWith { NavBackStack<NavKey>(it) },
    )

    private var visibleTabs: List<ScreenKey> = emptyList()

    private fun render(state: NavigationState = newState()): NavigationState {
        composeTestRule.setContent {
            val navigator = remember { Navigator(state) }
            val tabs = rememberVisibleTabs(navigator)
            visibleTabs = tabs
            Column {
                tabs.forEach { key -> Text(key.toString(), Modifier.testTag("tab_$key")) }
            }
        }
        composeTestRule.waitForIdle()
        return state
    }

    @Test
    fun planningTab_isNeverVisible_withoutAmapConfiguration() {
        render()
        composeTestRule.onNodeWithTag("tab_${ScreenKey.RoutePlan}").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tab_${ScreenKey.Home}").assertExists()
        composeTestRule.onNodeWithTag("tab_${ScreenKey.More}").assertExists()
        composeTestRule.runOnIdle {
            assertEquals(listOf(ScreenKey.Home, ScreenKey.More), visibleTabs)
        }
    }

    @Test
    fun restoredPlanningStack_returnsToHome_butRouteDetailStackRemainsUsable() {
        val state = newState()
        Navigator(state).run {
            open(ScreenKey.Search)
            selectTab(ScreenKey.RoutePlan)
            open(line)
        }
        render(state)
        composeTestRule.runOnIdle {
            assertEquals(ScreenKey.Home, state.topLevel)
            assertEquals(listOf<NavKey>(ScreenKey.RoutePlan), state.stacks.getValue(ScreenKey.RoutePlan).toList())
            assertEquals(ScreenKey.Search, state.currentKey)
        }
    }
}
