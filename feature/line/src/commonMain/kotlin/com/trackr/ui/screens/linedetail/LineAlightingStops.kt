package com.trackr.ui.screens.linedetail

import com.trackr.core.domain.arrival.ArrivalRules
import com.trackr.core.domain.arrival.ArrivalText
import com.trackr.core.model.BusStop
import com.trackr.core.model.LiveBus
import com.trackr.core.model.TrackingSessionState

/** order 是正在驶向或停靠的站；尚在站间行驶时，这一站仍可作为下车目标。 */
internal fun BusStop.isFutureAlightingStop(bus: LiveBus): Boolean =
    order > bus.order || (order == bus.order && !ArrivalRules.isBusStopped(bus))

/** 下车站还未选择：这里的「先到」以线路页当前的候车站为准，已过站的车仍可选。 */
internal data class BoardingBusOption(
    val bus: LiveBus,
    val stopsAway: Int?,
    val eta: Int?,
    val stoppedHere: Boolean
) {
    val passed: Boolean get() = stopsAway != null && stopsAway < 0

    val arrivalRank: Long get() = when {
        passed -> Long.MAX_VALUE
        stoppedHere -> -1L
        eta != null -> eta.toLong()
        stopsAway != null -> stopsAway.toLong() * ArrivalText.STOP_SECONDS_ESTIMATE
        else -> Long.MAX_VALUE - 1
    }
}

internal fun LineDetailUiState.boardingBusOptions(): List<BoardingBusOption> {
    val target = selectedStop?.order?.takeIf { it > 0 }
    val stops = static?.stops.orEmpty()
    val snapshot = liveForBoarding ?: return emptyList()
    val etaMatchesTarget = target != null && snapshot.etaTargetOrder == target
    val options = snapshot.buses.asSequence()
        .filter { bus -> bus.busId.isNotBlank() && stops.any { it.isFutureAlightingStop(bus) } }
        .distinctBy { it.busId }
        .map { bus ->
            val away = if (target != null && bus.order > 0) target - bus.order else null
            BoardingBusOption(
                bus = bus,
                stopsAway = away,
                eta = bus.etaSeconds.takeIf { etaMatchesTarget && it > 0 },
                stoppedHere = away == 0 && ArrivalRules.isBusStopped(bus)
            )
        }.toList()
    return if (sortBoardingByArrival && target != null) options.sortedBy { it.arrivalRank } else options
}

/** 同名站、环线和重复站序不靠名字或站序单独猜。 */
internal fun TrackingSessionState.destinationIn(stops: List<BusStop>): BusStop? {
    val sameOrder = stops.filter { it.order == targetOrder }
    return sameOrder.firstOrNull { it.stopId == targetStopId && it.name == targetStopName }
        ?: sameOrder.singleOrNull { it.stopId == targetStopId && targetStopId.isNotBlank() }
        ?: sameOrder.singleOrNull { it.name == targetStopName }
}

/** 某些上游的站 ID 就是站序；重复站序时仍要能分别选中不同站名。 */
internal val BusStop.alightingKey: String get() = "$order:${stopId.length}:$stopId:$name"
