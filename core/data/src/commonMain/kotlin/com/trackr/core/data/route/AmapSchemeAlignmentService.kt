package com.trackr.core.data.route

import com.trackr.core.data.provider.TransitProviderCoordinator
import com.trackr.core.data.repository.RealtimeRepository
import com.trackr.core.domain.math.RouteProjectionMath
import com.trackr.core.model.AmapTransitSchemeResult
import com.trackr.core.model.BusLineDirection
import com.trackr.core.model.BusStop
import com.trackr.core.model.CandidateLine
import com.trackr.core.model.EngineMode
import com.trackr.core.model.TransitCity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** 规划页方案列表最多显示几个（只对齐这几个；规划页和 [DefaultRoutePlanRepository] 共用） */
const val MAX_VISIBLE_SCHEMES = 4

/**
 * 仅供 [DefaultRoutePlanRepository] 使用的高德规划方案对齐。
 * 与普通路线的 [RouteAlignmentService] 分离，删除规划功能时不影响收藏、返程或追踪。
 *
 * @param coordinator 车来了搜索（不写搜索历史）；为 null 时保留原方案（测试用）
 */
class AmapSchemeAlignmentService(
    private val realtime: RealtimeRepository,
    private val coordinator: TransitProviderCoordinator? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        /** 方案与实时源对齐的最大并发 */
        private const val ALIGN_CONCURRENCY = 4
    }

    /** 方案线路与实时源的匹配结果：[fromStop]/[toStop] 为空表示只对齐了线路，没对齐站序 */
    private data class SchemeLineMatch(
        val direction: BusLineDirection,
        val fromStop: BusStop?,
        val toStop: BusStop?
    )

    /**
     * 把高德方案里的候选线路对齐到实时公交源（真实 lineId / 方向 / 站序）。
     * 只处理界面可见的前 [MAX_VISIBLE_SCHEMES] 个方案；同一「线路名 + 上车站 + 下车站」只查一次；
     * 并发不超过 [ALIGN_CONCURRENCY]。
     */
    suspend fun alignAmapSchemesWithRealtimeSource(
        schemes: List<AmapTransitSchemeResult>,
        city: TransitCity
    ): List<AmapTransitSchemeResult> {
        val coord = coordinator ?: return schemes
        val visible = schemes.take(MAX_VISIBLE_SCHEMES)
        val rest = schemes.drop(MAX_VISIBLE_SCHEMES)
        return withContext(ioDispatcher) {
            val semaphore = Semaphore(ALIGN_CONCURRENCY)
            val memo = LinkedHashMap<String, Deferred<SchemeLineMatch?>>()

            fun memoKey(lineName: String, fromCore: String, toCore: String) = "$lineName|$fromCore|$toCore"

            // 1. 去重后并发查询
            for (scheme in visible) {
                for (leg in scheme.group.legs) {
                    val fromCore = RouteProjectionMath.coreStopName(leg.fromStopName)
                    val toCore = RouteProjectionMath.coreStopName(leg.toStopName)
                    for (candidate in leg.candidateLines) {
                        val key = memoKey(candidate.lineName, fromCore, toCore)
                        if (!memo.containsKey(key)) {
                            val lineName = candidate.lineName
                            memo[key] = async {
                                semaphore.withPermit { matchSchemeLine(coord, city, lineName, fromCore, toCore) }
                            }
                        }
                    }
                }
            }

            // 2. 套用匹配结果
            val aligned = visible.map { scheme ->
                val newLegs = scheme.group.legs.map { leg ->
                    val fromCore = RouteProjectionMath.coreStopName(leg.fromStopName)
                    val toCore = RouteProjectionMath.coreStopName(leg.toStopName)
                    val updatedCandidates = leg.candidateLines.map { candidate ->
                        val match = memo[memoKey(candidate.lineName, fromCore, toCore)]?.await()
                        if (match != null) applySchemeLineMatch(candidate, match, city) else candidate
                    }
                    leg.copy(candidateLines = updatedCandidates)
                }
                scheme.copy(group = scheme.group.copy(legs = newLegs))
            }
            aligned + rest
        }
    }

    /** 在实时源里查找线路：站序能对上的方向优先，否则退而取第一个同名方向 */
    private suspend fun matchSchemeLine(
        coord: TransitProviderCoordinator,
        city: TransitCity,
        lineName: String,
        fromCore: String,
        toCore: String
    ): SchemeLineMatch? = runCatching {
        val searchRes = coord.search(city, lineName, EngineMode.CHELAILE)
        val matchedDirs = searchRes.lines.flatMap { it.directions }.filter { dir ->
            dir.name.contains(lineName) || lineName.contains(dir.name)
        }
        var fallback: SchemeLineMatch? = null
        for (dir in matchedDirs) {
            // 线路详情只走 realtime 一条路径，失败不再对同一上游重复请求
            val detail = realtime.getLineDetail(
                city = city,
                lineId = dir.lineId,
                source = dir.source,
                lineNameHint = dir.name,
                directionHint = dir.direction
            ).getOrNull()
            if (detail != null) {
                val fromStop = detail.stops.find {
                    it.name.contains(fromCore) || fromCore.contains(RouteProjectionMath.coreStopName(it.name))
                }
                val toStop = detail.stops.find {
                    it.name.contains(toCore) || toCore.contains(RouteProjectionMath.coreStopName(it.name))
                }
                if (fromStop != null && toStop != null && fromStop.order < toStop.order) {
                    return@runCatching SchemeLineMatch(dir, fromStop, toStop)
                }
            }
            if (fallback == null) {
                fallback = SchemeLineMatch(dir, null, null)
            }
        }
        fallback
    }.getOrNull()

    private fun applySchemeLineMatch(
        candidate: CandidateLine,
        match: SchemeLineMatch,
        city: TransitCity
    ): CandidateLine {
        val dir = match.direction
        val fromStop = match.fromStop
        val toStop = match.toStop
        return if (fromStop != null && toStop != null) {
            candidate.copy(
                cityId = city.cityId,
                cityName = city.cityName,
                lineId = dir.lineId,
                direction = dir.direction,
                source = dir.source,
                fromStopOrder = fromStop.order,
                toStopOrder = toStop.order,
                startStopName = fromStop.name,
                endStopName = toStop.name,
                mygolbsCityCode = city.mygolbsCityCode
            )
        } else {
            candidate.copy(
                cityId = city.cityId,
                cityName = city.cityName,
                lineId = dir.lineId,
                direction = dir.direction,
                source = dir.source,
                mygolbsCityCode = city.mygolbsCityCode
            )
        }
    }

}
