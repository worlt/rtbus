package com.trackr.feature.route

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trackr.core.navigation.TrackrNavigator
import com.trackr.core.navigation.ScreenKey
import com.trackr.ui.navigation.LocalAppViewModelStoreOwner
import com.trackr.ui.screens.routedetail.RouteDetailRoute
import com.trackr.ui.screens.routedetail.RouteDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

actual fun EntryProviderScope<NavKey>.routeEntries(navigator: TrackrNavigator) {
    // 普通路线详情仍可从收藏、通知和 deep link 打开；规划页 entry 已从应用装配移除。
    entry<ScreenKey.RouteDetail> { key ->
        val appOwner = LocalAppViewModelStoreOwner.current
        val routeDetailViewModel: RouteDetailViewModel = koinViewModel(viewModelStoreOwner = appOwner)
        RouteDetailRoute(groupId = key.groupId, viewModel = routeDetailViewModel, navigator = navigator)
    }
}
