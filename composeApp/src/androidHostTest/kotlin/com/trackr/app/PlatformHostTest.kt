package com.trackr.app

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trackr.core.designsystem.theme.TrackrTheme
import com.trackr.core.model.ThemeMode
import com.trackr.core.navigation.NavigationState
import com.trackr.core.navigation.Navigator
import com.trackr.core.navigation.ScreenKey
import com.trackr.core.navigation.rememberNavigationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** 平台壳不再管理地图。主题重组时仍须保留导航子树、普通路线详情和已捕获的 Navigator。 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlatformHostTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class Seen {
        var state: NavigationState? = null
        var navigator: Navigator? = null
        var rememberedToken: Any? = null
        var subtreeDisposals = 0
        var darkTheme = false
    }

    @Test
    fun themeFlip_keepsNavigationSubtreeRouteAndNavigator() {
        val seen = Seen()
        val themeMode = mutableStateOf(ThemeMode.Light)
        val route = ScreenKey.RouteDetail(groupId = "saved-route")

        composeTestRule.setContent {
            TrackrTheme(themeMode = themeMode.value) {
                PlatformHost {
                    val navigationState = rememberNavigationState(
                        topLevel = listOf(ScreenKey.Home, ScreenKey.More)
                    )
                    val navigator = remember(navigationState) { Navigator(navigationState) }
                    val token = remember { Any() }
                    val darkTheme = TrackrTheme.isDark
                    SideEffect {
                        seen.state = navigationState
                        seen.navigator = navigator
                        seen.rememberedToken = token
                        seen.darkTheme = darkTheme
                    }
                    DisposableEffect(Unit) { onDispose { seen.subtreeDisposals++ } }
                    Text("导航子树")
                }
            }
        }
        composeTestRule.waitForIdle()

        val originalState = requireNotNull(seen.state)
        val originalNavigator = requireNotNull(seen.navigator)
        val originalToken = requireNotNull(seen.rememberedToken)
        assertFalse(seen.darkTheme)
        composeTestRule.runOnIdle { originalNavigator.open(route) }
        composeTestRule.waitForIdle()
        assertEquals(route, seen.state?.currentKey)

        composeTestRule.runOnIdle { themeMode.value = ThemeMode.Dark }
        composeTestRule.waitForIdle()

        assertTrue(seen.darkTheme)
        assertSame("导航状态不能被重建", originalState, seen.state)
        assertSame("捕获的 Navigator 不能过期", originalNavigator, seen.navigator)
        assertSame("普通 remember 状态不能丢", originalToken, seen.rememberedToken)
        assertEquals("普通路线详情不能丢", route, seen.state?.currentKey)
        assertEquals("导航子树不能被销毁", 0, seen.subtreeDisposals)

        // 重组前捕获的回调仍能操作正在显示的栈，切去我的后公交栈上的详情仍保留。
        composeTestRule.runOnIdle { originalNavigator.selectTab(ScreenKey.More) }
        composeTestRule.waitForIdle()
        assertEquals(ScreenKey.More, seen.state?.topLevel)
        assertEquals(listOf(ScreenKey.Home, route), originalState.stacks.getValue(ScreenKey.Home).toList())
        assertEquals(0, seen.subtreeDisposals)
    }
}
