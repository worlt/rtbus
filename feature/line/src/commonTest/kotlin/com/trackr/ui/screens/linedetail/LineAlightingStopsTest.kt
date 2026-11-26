package com.trackr.ui.screens.linedetail

import kotlin.test.assertEquals
import com.trackr.core.model.BusLineDirection
import com.trackr.core.model.BusStop
import com.trackr.core.model.LiveBus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineAlightingStopsTest {
    private val stop = BusStop("s2", "下一站", 2)
    private val moving = LiveBus("bus", "测试车", order = 2, segmentProgress = 0.5f, wgsLat = 30.0, wgsLng = 120.0)

    @Test fun boardingOrderUsesTargetEtaWithStableStationFallbackAndKeepsPassedVehicles() {
        val stops = (1..7).map { BusStop("s$it", "站$it", it) }
        val line = BusLineDirection("L", "1路", startStop = "站1", endStop = "站7")
        fun bus(id: String, order: Int, eta: Int = -1, stopped: Boolean = false) =
            LiveBus(id, id, order, etaSeconds = eta, isArrived = stopped)
        val live = LineLive(listOf(bus("passed", 5, 1), bus("fallback", 3), bus("fast", 1, 85),
            bus("tie", 2, 85), bus("slow", 2, 178), bus("here", 4, stopped = true),
            bus("terminal", 7, stopped = true), bus("", 2), bus("fast", 3)), etaTargetOrder = 4)
        val state = LineDetailUiState(static = LineStatic(line, null, stops), selectedStop = stops[3], live = live)
        fun ids(s: LineDetailUiState) = s.boardingBusOptions().map { it.bus.busId }
        assertEquals(listOf("passed", "fallback", "fast", "tie", "slow", "here"), ids(state))
        val sorted = state.copy(sortBoardingByArrival = true)
        assertEquals(listOf("here", "fast", "tie", "fallback", "slow", "passed"), ids(sorted))
        val staleEta = sorted.copy(live = live.copy(etaTargetOrder = 3))
        assertTrue(staleEta.boardingBusOptions().all { it.eta == null })
        assertEquals(listOf("here", "fallback", "tie", "slow", "fast", "passed"), ids(staleEta))
        assertEquals(ids(state), ids(sorted.copy(selectedStop = null)))
    }

    @Test fun approachingStopRemainsAvailable() {
        assertTrue(stop.isFutureAlightingStop(moving))
        assertTrue(stop.isFutureAlightingStop(moving.copy(etaSeconds = 30)))
    }

    @Test fun stoppedAndPassedStopsAreUnavailable() {
        assertFalse(stop.isFutureAlightingStop(moving.copy(isArrived = true)))
        assertFalse(stop.isFutureAlightingStop(moving.copy(segmentProgress = 0.99f)))
        assertFalse(stop.isFutureAlightingStop(moving.copy(order = 3)))
    }

    @Test fun missingCoordinatesDoNotMakeDefaultProgressAnArrival() {
        assertTrue(stop.isFutureAlightingStop(moving.copy(wgsLat = 0.0, wgsLng = 0.0, segmentProgress = 1f)))
    }

    @Test fun laterStopIsAvailableEvenWhenVehicleIsStopped() {
        assertTrue(stop.copy(order = 3).isFutureAlightingStop(moving.copy(isArrived = true)))
    }
}
