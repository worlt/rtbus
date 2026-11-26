package com.trackr.ui.screens.linedetail

import com.trackr.core.model.BusStop
import com.trackr.core.model.LiveBus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineAlightingStopsTest {
    private val stop = BusStop("s2", "下一站", 2)
    private val moving = LiveBus("bus", "测试车", order = 2, segmentProgress = 0.5f, wgsLat = 30.0, wgsLng = 120.0)

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
