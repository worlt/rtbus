// :feature:route：普通路线详情、实时到站与追踪。共用依赖由 trackr.kmp.feature 提供。
plugins {
    alias(libs.plugins.trackr.kmp.feature)
}

kotlin {
    android {
        // 主机测试里也要能读到 :core:designsystem 的 Compose 资源（图标），否则 vectorResource 一直等不到
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            // 路线详情使用共享图标与本模块的文案资源
            implementation(libs.compose.mp.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.trackr.feature.route.resources"
}
