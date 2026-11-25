package com.trackr.feature.route

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trackr.core.data.repository.AmapRouteGateway
import com.trackr.core.domain.platform.AmapSdkKeyApplier
import com.trackr.core.domain.platform.MapSdkInitializer
import com.trackr.core.navigation.TrackrNavigator
import com.trackr.core.navigation.ScreenKey
import com.trackr.data.remote.amap.AmapSdkTransitService
import com.trackr.data.remote.amap.AndroidAmapRouteGateway
import com.trackr.data.remote.amap.AndroidAmapSdkKeyApplier
import com.trackr.data.remote.amap.AndroidMapSdkInitializer
import com.trackr.ui.navigation.LocalAppViewModelStoreOwner
import com.trackr.ui.screens.routedetail.RouteDetailRoute
import com.trackr.ui.screens.routedetail.RouteDetailViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module

actual fun EntryProviderScope<NavKey>.routeEntries(navigator: TrackrNavigator) {
    // 普通路线详情仍可从收藏、通知和 deep link 打开；规划页 entry 已从应用装配移除。
    entry<ScreenKey.RouteDetail> { key ->
        val appOwner = LocalAppViewModelStoreOwner.current
        val routeDetailViewModel: RouteDetailViewModel = koinViewModel(viewModelStoreOwner = appOwner)
        RouteDetailRoute(groupId = key.groupId, viewModel = routeDetailViewModel, navigator = navigator)
    }
}

/** 高德 Android SDK 相关绑定暂留，后续高德实现清理阶段删除。 */
val routeAndroidModule = module {
    single<AmapRouteGateway> {
        AndroidAmapRouteGateway(
            amapSdkService = runCatching { AmapSdkTransitService(androidContext()) }.getOrNull(),
            amapApiClient = get(),
            amapKeys = get()
        )
    }
    single<AmapSdkKeyApplier> { AndroidAmapSdkKeyApplier(androidContext()) }
    single<MapSdkInitializer> { AndroidMapSdkInitializer(androidContext()) }
}
