package com.trackr.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * 顶层页签（每个页签一条返回栈），3 个：公交、路线、我的。
 * Key 沿用旧名（[ScreenKey.Home] / [ScreenKey.RoutePlan] / [ScreenKey.More]），底栏文字在 `:composeApp` 的 `TrackrRoot` 里。
 * 「收藏」不是页签：编辑收藏是首页栈的子页（[ScreenKey.FavoritesEdit]）；[ScreenKey.FavoritesGroup] 的页面已删，只剩 Key。
 */
object TopLevel {
    /** 起始页签：非起始页签在根页按返回回到这里；它的根页再按返回就退出（exit-through-home）。 */
    val start: ScreenKey = ScreenKey.Home

    /** 底栏顺序。 */
    val tabs: List<ScreenKey> = listOf(
        ScreenKey.Home,
        ScreenKey.RoutePlan,
        ScreenKey.More,
    )

    fun isTopLevel(key: NavKey?): Boolean = key != null && key in tabs

    /**
     * 当前底栏只显示公交和我的；旧规划标识仅用于恢复历史存档。
     * 顺序同 [tabs]。页签集合变了交给 [Navigator.onTabsChanged]（在被去掉的页签上就清栈回起始页签）。
     */
    fun visibleTabs(): List<ScreenKey> = tabs.filterNot { it == ScreenKey.RoutePlan }
}
