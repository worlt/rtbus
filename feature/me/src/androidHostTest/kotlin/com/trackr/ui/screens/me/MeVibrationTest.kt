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
        compose.runOnIdle { state.value = state.value.copy(vibrationTestResult = "已发送到站引导震动请求，不播放声音。请以手机是否实际震动为准。\n\n反馈实现：导航引导兼容版 v2\n设备：测试设备\nAndroid：15 / API 35\n铃声模式：静音；铃声音量：0\n勿扰：关闭\n应用震动：true；通知允许：true\n渠道重要级别：4；渠道震动：true\n请求结果：REQUESTED") }
        compose.onNodeWithText("震动测试结果").assertIsDisplayed()
        System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
            val file = java.io.File(directory, "balanced-vibration-test-dialog.png")
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

    @Test fun unsupportedPlatformDoesNotShowTestEntry() {
        compose.setContent { TrackrTheme { MeScreen(MeUiState(), {}) } }
        compose.onNodeWithText("测试震动").assertDoesNotExist()
    }
}
