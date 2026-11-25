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
 * 它不再作为可见入口返回，因此没有高德 Key 也不会误触发规划页或隐私流程。
 */
@Composable
fun rememberVisibleTabs(navigator: Navigator): List<ScreenKey> {
    // 传 false 复用现有 Navigator 对旧 RoutePlan 栈的清理语义，同时不改动序列化模型。
    val tabs = remember { TopLevel.visibleTabs(hasMapKey = false) }
    LaunchedEffect(navigator, tabs) {
        navigator.onTabsChanged(tabs.toSet())
    }
    return tabs
}
