package com.trackr.app.di

import org.koin.core.module.Module
import org.koin.dsl.module
import com.trackr.app.navigation.AppTabBar
import com.trackr.app.navigation.MaterialAppTabBar

actual val platformUiModule: Module = module { single<AppTabBar> { MaterialAppTabBar() } }

/** Android 平台额外 UI 注入；高德规划模块已从应用装配移除。 */
internal actual val platformFeatureModules: List<Module> = listOf(
    platformUiModule,
)
