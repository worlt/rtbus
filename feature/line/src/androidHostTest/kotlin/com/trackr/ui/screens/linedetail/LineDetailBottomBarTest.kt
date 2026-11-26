package com.trackr.ui.screens.linedetail

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import com.trackr.core.model.LiveBus
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.trackr.core.designsystem.theme.TrackrTheme
import com.trackr.core.model.BusLineDirection
import com.trackr.core.model.BusStop
import com.trackr.core.model.TrackingSessionState
import com.trackr.core.model.RailMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 换向从方向行挪到底栏：反向线路存在时是常驻圆钮，单程线时不占位；
 * 换向请求进行中转圈并禁用，挡住连点。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineDetailBottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stationOverviewAlwaysShowsBoardedEntry() {
        composeTestRule.setContent {
            TrackrTheme { LineDetailScreen(state = stateOf(reverseLine), onEvent = {}) }
        }
        composeTestRule.onNodeWithText("我已上车 · 选择下车站").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun boardedEntryIsVisibleAndClickableOnSmallScreen() {
        var event: LineDetailEvent? = null
        show(stateOf(reverseLine).copy(live = LineLive(listOf(
            LiveBus(busId = "selected", plateOrFleetNo = "粤R08738D", order = 2)
        )))) { event = it }
        composeTestRule.onNodeWithTag("btn_boarded").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(LineDetailEvent.OpenAlightingPicker, event)
        System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
            val file = java.io.File(directory, "silent-boarding-footer.png")
            file.parentFile?.mkdirs()
            file.outputStream().use {
                composeTestRule.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun boardedAndTrackEntriesFitOnCompactPhone() {
        show(stateOf(reverseLine).copy(live = LineLive(listOf(
            LiveBus(busId = "selected", plateOrFleetNo = "粤R08738D", order = 2)
        ))))
        composeTestRule.onNodeWithTag("btn_boarded").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithText("追踪这班车").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("换向").assertIsDisplayed()
    }

    @Test
    fun onboardShowsOneEndActionAndMarksDestinationNotTheBrowsingStop() {
        val ride = TrackingSessionState(isActive = true, lineId = line.lineId, pinnedBusId = "b1", isOnboard = true,
            targetStopId = "s5", targetOrder = 5, targetStopName = "第5站", stopsRemaining = 3, etaSeconds = 360)
        show(stateOf(reverseLine).copy(panelMode = PanelMode.BUS_FOLLOW, followedBusId = "b1", trackingSession = ride,
            live = LineLive(listOf(LiveBus(busId = "b1", plateOrFleetNo = "粤R01508D", order = 2)))))
        composeTestRule.onNodeWithText("正在追这辆车 · 结束").assertDoesNotExist()
        composeTestRule.onNodeWithText("结束追踪").assertIsDisplayed()
        composeTestRule.onNodeWithText("更改下车站").assertIsDisplayed()
        composeTestRule.onNodeWithText("下车站：第5站").assertIsDisplayed()
        composeTestRule.onNodeWithText("我的下车站").assertExists()
        composeTestRule.onNodeWithText("3 站").assertIsDisplayed()
        composeTestRule.onNodeWithText("约 6 分钟").assertIsDisplayed()
        composeTestRule.onNodeWithText("到终点预测").assertDoesNotExist()
    }

    @Test
    fun missingLiveBusKeepsPlateDestinationAndEndActionButNotStaleEta() {
        val ride = TrackingSessionState(isActive = true, lineId = line.lineId, pinnedBusId = "b1", isOnboard = true,
            pinnedPlate = "粤R01508D", targetStopId = "s5", targetOrder = 5, targetStopName = "第5站", etaSeconds = 360)
        show(stateOf(reverseLine).copy(panelMode = PanelMode.BUS_FOLLOW, followedBusId = "b1", trackingSession = ride))
        composeTestRule.onNodeWithText("单车跟随 · 粤R01508D").assertIsDisplayed()
        composeTestRule.onNodeWithText("下车站：第5站").assertIsDisplayed()
        composeTestRule.onNodeWithText("结束追踪").assertIsDisplayed()
        composeTestRule.onNodeWithText("更改下车站").assertIsNotEnabled()
        composeTestRule.onNodeWithText("约 6 分钟").assertDoesNotExist()
    }

    @Test
    fun overviewShortcutOpensTheRideWithoutChoosingABusAgain() {
        var event: LineDetailEvent? = null
        show(stateOf(reverseLine).copy(rideToResume = RideShortcut("ride-2", "第5站"))) { event = it }
        composeTestRule.onNodeWithText("返回单车跟随 · 到第5站下车").assertIsDisplayed().performClick()
        assertEquals(LineDetailEvent.ResumeRide("ride-2"), event)
    }

    private val line = BusLineDirection(
        lineId = "line_001",
        name = "302路",
        direction = 0,
        startStop = "科技园",
        endStop = "火车站",
        firstTime = "05:30",
        lastTime = "23:30"
    )

    private val reverseLine = line.copy(
        lineId = "line_002",
        direction = 1,
        startStop = "火车站",
        endStop = "科技园"
    )

    private val stops = (1..10).map { order ->
        BusStop(stopId = "s$order", name = "第${order}站", order = order)
    }

    private fun stateOf(
        reverse: BusLineDirection?,
        isSwitchingDirection: Boolean = false,
        showTransitCode: Boolean = true
    ) = LineDetailUiState(
        lineNameHint = line.name,
        isLoading = false,
        static = LineStatic(line = line, reverseDirection = reverse, stops = stops),
        selectedStop = stops[2],
        live = LineLive(buses = emptyList()),
        railMode = RailMode.Horizontal,
        isSwitchingDirection = isSwitchingDirection,
        showTransitCodeButton = showTransitCode
    )

    private fun show(state: LineDetailUiState, onEvent: (LineDetailEvent) -> Unit = {}) {
        composeTestRule.setContent {
            TrackrTheme {
                LineDetailScreen(state = state, onEvent = onEvent)
            }
        }
    }

    @Test
    fun reverseDirectionExists_showsSwitchButtonInBottomBar() {
        show(stateOf(reverse = reverseLine))

        composeTestRule.onNodeWithContentDescription("换向").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("换向").assertIsEnabled()
        // 换向挪走后，方向行只剩站名；底栏另外两个动作还在
        composeTestRule.onNodeWithText("科技园 → 火车站").assertIsDisplayed()
    }

    @Test
    fun oneWayLine_hidesSwitchButton() {
        show(stateOf(reverse = null))

        composeTestRule.onNodeWithContentDescription("换向").assertDoesNotExist()
    }

    @Test
    fun reverseWithoutLineId_hidesSwitchButton() {
        // 脏数据：反向对象在但 lineId 为空，点了也请求不出去，当没有处理
        show(stateOf(reverse = reverseLine.copy(lineId = "")))

        composeTestRule.onNodeWithContentDescription("换向").assertDoesNotExist()
    }

    @Test
    fun switching_disablesSwitchButton() {
        show(stateOf(reverse = reverseLine, isSwitchingDirection = true))

        composeTestRule.onNodeWithContentDescription("换向").assertIsNotEnabled()
    }

    @Test
    fun clickSwitch_sendsSwitchDirectionEvent() {
        var lastEvent: LineDetailEvent? = null
        show(stateOf(reverse = reverseLine)) { lastEvent = it }

        composeTestRule.onNodeWithContentDescription("换向").performClick()

        assertEquals(LineDetailEvent.SwitchDirection, lastEvent)
    }

    // ---- 乘车码按钮跟着「我的」里的开关 ----

    @Test
    fun transitCodeButton_shownByDefault() {
        show(stateOf(reverse = reverseLine))

        composeTestRule.onNodeWithContentDescription("乘车码").assertIsDisplayed()
    }

    @Test
    fun transitCodeButton_hiddenWhenSettingOff() {
        // 「我的」里关掉「显示乘车码按钮」：首页悬浮按钮去掉后，线路页底栏这个圆钮也不留
        show(stateOf(reverse = reverseLine, showTransitCode = false))

        composeTestRule.onNodeWithContentDescription("乘车码").assertDoesNotExist()
        // 其余动作照旧
        composeTestRule.onNodeWithText("追踪这班车").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("换向").assertIsDisplayed()
    }
}
