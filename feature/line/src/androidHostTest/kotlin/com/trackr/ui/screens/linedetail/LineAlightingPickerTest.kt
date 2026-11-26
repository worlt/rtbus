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
import com.trackr.ui.components.sort.LineSortToggleTags
import org.junit.Assert.assertTrue
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

    @Test fun overviewVehiclePickerRequiresExplicitBusSelection() {
        var event: LineDetailEvent? = null
        val current = state().copy(boardingBusId = null, followedBusId = null,
            panelMode = PanelMode.STATION_CARD, isBoardingBusPickerOpen = true,
            live = LineLive(listOf(
                LiveBus(busId = "one", plateOrFleetNo = "粤R11111", order = 1),
                LiveBus(busId = "two", plateOrFleetNo = "粤R22222", order = 2),
                LiveBus(busId = "terminal", plateOrFleetNo = "粤R33333", order = 4, isArrived = true)
            )))
        compose.setContent { TrackrTheme { LineDetailScreen(current, { event = it }) } }
        compose.onNodeWithTag("boarding_bus_terminal").assertDoesNotExist()
        compose.onNodeWithTag("boarding_bus_two").performClick()
        assertEquals(LineDetailEvent.SelectBoardingBus("two"), event)
    }

    @Test fun sortMenuReordersCandidatesWithoutPicking_andTapUsesTheVisibleBusId() {
        val current = mutableStateOf(state().copy(boardingBusId = null, followedBusId = null,
            panelMode = PanelMode.STATION_CARD, isBoardingBusPickerOpen = true,
            live = LineLive(listOf(LiveBus("slow", "慢车", 1, etaSeconds = 150),
                LiveBus("fast", "快车", 1, etaSeconds = 65)), etaTargetOrder = 2)))
        val events = mutableListOf<LineDetailEvent>()
        compose.setContent { TrackrTheme { LineDetailScreen(current.value, { event ->
            events += event
            if (event is LineDetailEvent.SetBoardingSortByArrival) current.value = current.value.copy(sortBoardingByArrival = event.enabled)
        }) } }
        fun top(id: String) = compose.onNodeWithTag("boarding_bus_$id").fetchSemanticsNode().boundsInRoot.top
        assertTrue(top("slow") < top("fast"))
        compose.onNodeWithTag(LineSortToggleTags.TRIGGER).performClick()
        compose.onNodeWithTag(LineSortToggleTags.BY_ARRIVAL).performClick()
        assertTrue(top("fast") < top("slow"))
        compose.onNodeWithTag("boarding_bus_fast").performClick()
        assertEquals(listOf(LineDetailEvent.SetBoardingSortByArrival(true), LineDetailEvent.SelectBoardingBus("fast")), events)
        compose.onNodeWithTag(LineSortToggleTags.TRIGGER).performClick()
        compose.onNodeWithTag(LineSortToggleTags.DEFAULT).performClick()
        assertTrue(top("slow") < top("fast"))
    }

    @Test fun cancellingVehicleSelectionStartsNothing() {
        var event: LineDetailEvent? = null
        val current = state().copy(boardingBusId = null, isBoardingBusPickerOpen = true)
        compose.setContent { TrackrTheme { LineDetailScreen(current, { event = it }) } }
        compose.onNodeWithText("取消").performClick()
        assertEquals(LineDetailEvent.DismissAlightingPicker, event)
    }

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

    @Test fun failedSnapshotDisablesOpenDestinationDialogUntilNewSnapshot() {
        val current = mutableStateOf(state())
        compose.setContent { TrackrTheme { LineDetailScreen(current.value, {}) } }
        compose.onNodeWithTag("alight_stop_3").performClick()
        compose.onNodeWithTag("btn_start_alighting").assertIsEnabled()
        compose.runOnIdle { current.value = current.value.copy(invalidBoardingSnapshotThrough = 0L) }
        compose.onNodeWithTag("alight_stop_3").assertDoesNotExist()
        compose.onNodeWithTag("btn_start_alighting").assertIsNotEnabled()
        compose.runOnIdle { current.value = current.value.copy(live = current.value.live!!.copy(updatedAtMillis = 1L)) }
        compose.onNodeWithTag("btn_start_alighting").assertIsEnabled()
    }

    @Test fun failedSnapshotRemovesVehicleChoicesEvenWhenSortAndBusListAreUnchanged() {
        val current = mutableStateOf(state().copy(boardingBusId = null, followedBusId = null,
            panelMode = PanelMode.STATION_CARD, isBoardingBusPickerOpen = true))
        compose.setContent { TrackrTheme { LineDetailScreen(current.value, {}) } }
        compose.onNodeWithTag("boarding_bus_b").assertExists()
        compose.runOnIdle { current.value = current.value.copy(invalidBoardingSnapshotThrough = 0L) }
        compose.onNodeWithTag("boarding_bus_b").assertDoesNotExist()
        compose.onNodeWithText("取消").performClick()
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
