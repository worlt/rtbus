package com.trackr.ui.screens.me

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.trackr.core.designsystem.theme.TrackrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rCN-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MeVibrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun testEntryShowsReportWithSettingsAndDismissActions() {
        val state = mutableStateOf(MeUiState(canTestVibration = true))
        val events = mutableListOf<MeEvent>()
        compose.setContent { TrackrTheme { MeScreen(state.value, onEvent = { events += it }) } }
        compose.onNodeWithTag("me_settings_list").performScrollToNode(hasText("测试震动"))
        compose.onNodeWithText("测试震动").assertIsDisplayed().performClick()
        assertEquals(listOf(MeEvent.TestVibration), events)
        compose.runOnIdle { state.value = state.value.copy(vibrationTestResult = "已发送到站引导震动请求，不播放声音。请以手机是否实际震动为准。\n\n反馈实现：导航引导兼容版 v2\n到站反馈：确认到站与主动引导 v4\n设备：测试设备\nAndroid：15 / API 35\n铃声模式：静音；铃声音量：0\n勿扰：关闭\n应用震动：true；通知允许：true\n渠道重要级别：4；渠道震动：true\n请求结果：REQUESTED\n\n最近真实提醒（最多6条，不含测试）：\n11-26 15:00:00 APPROACHING 通知=false 震动=REQUESTED\n11-26 15:00:10 ARRIVED 通知=true 震动=REQUESTED\n\n最近追踪状态：\n11-26 15:00:10 本次追踪已自动结束") }
        compose.onNodeWithText("提醒测试结果").assertIsDisplayed()
        System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
            val file = java.io.File(directory, "arrival-v4-test-result-dialog.png")
            file.parentFile?.mkdirs()
            file.outputStream().use {
                compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.onNodeWithText("系统提醒设置").performClick()
        assertEquals(MeEvent.OpenAlertChannelSettings, events.last())
        compose.onNodeWithText("关闭").performClick()
        assertEquals(MeEvent.DismissVibrationTest, events.last())
    }

    @Test fun arrivalEntryIsVisibleAndSendsTheArrivalEvent() {
        val events = mutableListOf<MeEvent>()
        compose.setContent { TrackrTheme { MeScreen(MeUiState(canTestArrival = true), { events += it }) } }
        compose.onNodeWithTag("me_settings_list").performScrollToNode(hasText("测试到站提醒"))
        compose.onNodeWithText("测试到站提醒").assertIsDisplayed().performClick()
        assertEquals(listOf(MeEvent.TestArrivalAlert), events)
        System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
            java.io.File(directory, "arrival-v4-test-entry.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test fun unsupportedPlatformDoesNotShowTestEntry() {
        compose.setContent { TrackrTheme { MeScreen(MeUiState(), {}) } }
        compose.onNodeWithText("测试震动").assertDoesNotExist()
        compose.onNodeWithText("测试到站提醒").assertDoesNotExist()
    }
}
