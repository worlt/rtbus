// 键值存储：DataStore KMP（Android + iosArm64 + iosSimulatorArm64），重构计划 §2.4 / 第 9 步。
// 两个活动文件：trackr_settings（设置，参与备份）、tracking_session（追踪会话，备份排除）。
// 历史 trackr_amap_keys 文件不再打开或创建，Android 仍保留备份排除。
plugins {
    alias(libs.plugins.trackr.kmp.library)
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(libs.androidx.datastore.preferences.core)
            api(libs.androidx.datastore.core.okio)
            api(libs.okio)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.junit)
        }
    }
}
