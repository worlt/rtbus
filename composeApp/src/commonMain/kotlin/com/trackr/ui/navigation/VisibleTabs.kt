package com.trackr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.trackr.core.navigation.Navigator
import com.trackr.core.navigation.ScreenKey
import com.trackr.core.navigation.TopLevel

/**
 * RTBus 当前的应用页签：公交和我的。
 *
 * [ScreenKey.RoutePlan] 仍保留在核心导航模型中，用于读取旧版保存状态和 deep link 时的兼容；
 * 它不再作为可见入口返回，因此不会误触发退役的规划页。
 */
@Composable
fun rememberVisibleTabs(navigator: Navigator): List<ScreenKey> {
    // 可见页签固定；继续清理可能恢复出的旧 RoutePlan 当前页签。
    val tabs = remember { TopLevel.visibleTabs() }
    LaunchedEffect(navigator, tabs) {
        navigator.onTabsChanged(tabs.toSet())
    }
    return tabs
}
