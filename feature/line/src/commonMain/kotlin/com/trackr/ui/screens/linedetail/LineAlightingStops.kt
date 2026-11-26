package com.trackr.ui.screens.linedetail

import com.trackr.core.domain.arrival.ArrivalRules
import com.trackr.core.model.BusStop
import com.trackr.core.model.LiveBus

/** order 是正在驶向或停靠的站；尚在站间行驶时，这一站仍可作为下车目标。 */
internal fun BusStop.isFutureAlightingStop(bus: LiveBus): Boolean =
    order > bus.order || (order == bus.order && !ArrivalRules.isBusStopped(bus))
