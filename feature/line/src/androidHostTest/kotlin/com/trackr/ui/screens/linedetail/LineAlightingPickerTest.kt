package com.trackr.ui.screens.linedetail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.trackr.core.designsystem.theme.TrackrTheme
import com.trackr.core.model.BusLineDirection
import com.trackr.core.model.BusStop
import com.trackr.core.model.LiveBus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineAlightingPickerTest {
    @get:Rule val compose = createComposeRule()
    private val stops = (1..4).map { BusStop("s" + it, "站" + it, it) }
    private val line = BusLineDirection(lineId = "l", name = "1路", direction = 0, startStop = "站1", endStop = "站4")
    private fun state(order: Int = 2, isStopped: Boolean = true) = LineDetailUiState(
        isLoading = false,
        static = LineStatic(line, null, stops),
        live = LineLive(buses = listOf(LiveBus(busId = "b", plateOrFleetNo = "测试车", order = order, isArrived = isStopped))),
        selectedStop = stops[1], followedBusId = "b", boardingBusId = "b", panelMode = PanelMode.BUS_FOLLOW
    )

    @Test fun pickerOnlyOffersFutureStopsAndRequiresConfirmation() {
        var event: LineDetailEvent? = null
        compose.setContent { TrackrTheme { LineDetailScreen(state(), { event = it }) } }
        compose.onNodeWithTag("alight_stop_1").assertDoesNotExist()
        compose.onNodeWithTag("alight_stop_2").assertDoesNotExist()
        compose.onNodeWithTag("btn_start_alighting").assertIsNotEnabled()
        compose.onNodeWithTag("alight_stop_3").performClick()
        compose.onNodeWithTag("btn_start_alighting").assertIsEnabled().performClick()
        assertEquals(LineDetailEvent.StartAlightingReminder(stops[2]), event)
    }

    @Test fun vehiclePassingSelectedDestinationInvalidatesConfirmation() {
        val current = mutableStateOf(state())
        compose.setContent { TrackrTheme { LineDetailScreen(current.value, {}) } }
        compose.onNodeWithTag("alight_stop_3").performClick()
        compose.onNodeWithTag("btn_start_alighting").assertIsEnabled()
        compose.runOnIdle { current.value = state(order = 3) }
        compose.onNodeWithTag("alight_stop_3").assertDoesNotExist()
        compose.onNodeWithTag("btn_start_alighting").assertIsNotEnabled()
    }

    @Test fun approachingStationIsStillAValidDestination() {
        var event: LineDetailEvent? = null
        compose.setContent { TrackrTheme { LineDetailScreen(state(isStopped = false), { event = it }) } }
        compose.onNodeWithTag("alight_stop_2").performClick()
        compose.onNodeWithTag("btn_start_alighting").assertIsEnabled().performClick()
        assertEquals(LineDetailEvent.StartAlightingReminder(stops[1]), event)
    }

    @Test fun dismissDoesNotStartAnAlightingReminder() {
        var event: LineDetailEvent? = null
        compose.setContent { TrackrTheme { LineDetailScreen(state(), { event = it }) } }
        compose.onNodeWithText("取消").performClick()
        assertEquals(LineDetailEvent.DismissAlightingPicker, event)
    }
}
