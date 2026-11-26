package com.trackr.ui.screens.linedetail

import com.trackr.core.domain.arrival.ArrivalRules
import com.trackr.core.model.BusStop
import com.trackr.core.model.LiveBus
import com.trackr.core.model.TrackingSessionState

/** order 是正在驶向或停靠的站；尚在站间行驶时，这一站仍可作为下车目标。 */
internal fun BusStop.isFutureAlightingStop(bus: LiveBus): Boolean =
    order > bus.order || (order == bus.order && !ArrivalRules.isBusStopped(bus))

/** 同名站、环线和重复站序不靠名字或站序单独猜。 */
internal fun TrackingSessionState.destinationIn(stops: List<BusStop>): BusStop? {
    val sameOrder = stops.filter { it.order == targetOrder }
    return sameOrder.firstOrNull { it.stopId == targetStopId && it.name == targetStopName }
        ?: sameOrder.singleOrNull { it.stopId == targetStopId && targetStopId.isNotBlank() }
        ?: sameOrder.singleOrNull { it.name == targetStopName }
}

/** 某些上游的站 ID 就是站序；重复站序时仍要能分别选中不同站名。 */
internal val BusStop.alightingKey: String get() = "$order:${stopId.length}:$stopId:$name"
