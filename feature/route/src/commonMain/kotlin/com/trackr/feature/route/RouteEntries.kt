package com.trackr.feature.route

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.trackr.core.navigation.TrackrNavigator
import com.trackr.core.navigation.ScreenKey
import com.trackr.ui.screens.routedetail.RouteDetailViewModel
import com.trackr.ui.screens.routeplan.RoutePlanViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 路线相关导航入口（`:composeApp` 组装）。
 *
 * RTBus 当前只把普通路线详情（包括追踪 / 实时到站）接入应用导航；原高德规划页
 * 的实现仍保留在 feature 内，待后续拆除其数据层和平台实现。保留 [ScreenKey.RoutePlan]
 * 的序列化兼容性，但不再注册规划页 entry，避免入口层继续依赖高德功能。
 */
expect fun EntryProviderScope<NavKey>.routeEntries(navigator: TrackrNavigator)

/**
 * 路线相关的 Koin 定义。规划 ViewModel 暂时保留以便本轮小步拆分和既有测试继续编译；
 * 应用根不再提前创建它，后续移除规划实现时一并删除。
 */
val routeModule = module {
    viewModel {
        RoutePlanViewModel(
            routePlan = get(),
            settings = get(),
            realtime = get(),
            userLocationStore = get(),
            groupRepository = get(),
            groupSaver = get(),
            amapKeys = get(),
            locationRepository = get()
        )
    }
    viewModel {
        RouteDetailViewModel(
            groupRepository = get(),
            settings = get(),
            realtime = get(),
            alignment = get(),
            trackingRepository = get(),
            trackingController = get()
        )
    }
}
