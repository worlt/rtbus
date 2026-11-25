package com.trackr.core.data.route

import com.trackr.core.model.BusLineDetail
import com.trackr.core.model.BusLineDirection
import com.trackr.core.model.BusLineSummary
import com.trackr.core.model.BusStop
import com.trackr.core.model.CandidateLine
import com.trackr.core.model.TransitCity
import com.trackr.core.model.TransitGroup
import com.trackr.core.model.TransitLeg
import com.trackr.core.model.TransitSearchResult
import com.trackr.core.testing.FakeRealtimeRepository
import com.trackr.core.testing.FakeSettingsRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/** 普通线路能力不依赖规划仓库或高德方案，裁剪规划代码后仍必须保留。 */
class RouteAlignmentServiceTest {
    private val city = TransitCity("034", "上海")
    private val settings = FakeSettingsRepository(city)
    private val realtime = FakeRealtimeRepository()
    private var now = 1_000L
    private val synthetic = CandidateLine("", lineId = "line_synthetic", lineName = "1路")
    private val leg = TransitLeg(
        legIndex = 0, fromStopName = "甲站", toStopName = "乙站", candidateLines = listOf(synthetic)
    )
    private val forward = BusLineDirection("forward", "1路", startStop = "甲站", endStop = "乙站")
    private val reverse = forward.copy(lineId = "reverse", direction = 1, startStop = "乙站", endStop = "甲站")

    private fun provideLine() {
        realtime.searchResult = Result.success(TransitSearchResult(
            keyword = "1路", lines = listOf(BusLineSummary("1路", "1", directions = listOf(forward)))
        ))
        realtime.lineDetailsMap[forward.lineId] = BusLineDetail(
            line = forward, reverseDirection = reverse,
            stops = listOf(BusStop("a", "甲站", 2), BusStop("b", "乙站", 6)), buses = emptyList()
        )
        realtime.lineDetailsMap[reverse.lineId] = BusLineDetail(
            line = reverse, stops = listOf(BusStop("b", "乙站", 3), BusStop("a", "甲站", 7)), buses = emptyList()
        )
    }

    @Test
    fun ordinaryMatching_findsReverseDirection_withoutPlanningRepository() = runTest {
        provideLine()
        val service = RouteAlignmentService(settings, realtime, ioDispatcher = StandardTestDispatcher(testScheduler))

        val matched = assertNotNull(service.findRealtimeLineMatch(city, "1路", "乙站", "甲站", 1))

        assertEquals("reverse", matched.lineId)
        assertEquals(1, matched.direction)
        assertEquals(3, matched.fromStopOrder)
        assertEquals(7, matched.toStopOrder)
        assertEquals(city.cityId, matched.cityId)
        assertEquals(listOf("forward", "reverse"), realtime.lineDetailCalls)
    }

    @Test
    fun groupAlignment_updatesOnlySyntheticCandidates_andPreservesUnmatchedOnes() = runTest {
        provideLine()
        val real = synthetic.copy(lineId = "saved", fromStopOrder = 2, toStopOrder = 6)
        val unmatched = synthetic.copy(lineName = "99路")
        val group = TransitGroup("saved", "收藏路线", legs = listOf(leg.copy(candidateLines = listOf(synthetic, real, unmatched))))
        val service = RouteAlignmentService(settings, realtime, ioDispatcher = StandardTestDispatcher(testScheduler))

        val aligned = service.alignTransitGroupWithRealtimeSource(group)
        val candidates = aligned.legs.single().candidateLines

        assertEquals("forward", candidates[0].lineId)
        assertSame(real, candidates[1])
        assertSame(unmatched, candidates[2])
        assertEquals(group, aligned.copy(legs = group.legs))
        assertEquals(listOf("forward"), realtime.lineDetailCalls)
    }

    @Test
    fun hitCache_expiresAtTwentyFourHours_andSeparatesCitiesDirectionsAndStops() = runTest {
        provideLine()
        val service = RouteAlignmentService(
            settings, realtime, ioDispatcher = StandardTestDispatcher(testScheduler), wallClockMillis = { now }
        )
        val first = assertNotNull(service.alignSyntheticCandidateCached(leg, synthetic))
        now += 24 * 60 * 60 * 1000L - 1
        assertEquals(first, service.alignSyntheticCandidateCached(leg, synthetic))
        assertEquals(1, realtime.lineDetailCalls.size)
        now++
        assertNotNull(service.alignSyntheticCandidateCached(leg, synthetic))
        assertEquals(2, realtime.lineDetailCalls.size)

        assertNotNull(service.alignSyntheticCandidateCached(leg, synthetic.copy(cityId = "0755", cityName = "深圳")))
        assertEquals(3, realtime.lineDetailCalls.size)
        assertNotNull(service.alignSyntheticCandidateCached(leg, synthetic.copy(direction = 1)))
        assertEquals(4, realtime.lineDetailCalls.size)
        assertNotNull(service.alignSyntheticCandidateCached(leg.copy(fromStopName = "乙站", toStopName = "甲站"), synthetic))
        assertEquals(6, realtime.lineDetailCalls.size)
    }

    @Test
    fun missCache_retriesAtTenMinutes_withoutInventingStopOrders() = runTest {
        val service = RouteAlignmentService(
            settings, realtime, ioDispatcher = StandardTestDispatcher(testScheduler), wallClockMillis = { now }
        )
        assertNull(service.alignSyntheticCandidateCached(leg, synthetic))
        provideLine()
        now += 10 * 60 * 1000L - 1
        assertNull(service.alignSyntheticCandidateCached(leg, synthetic))
        assertEquals(0, realtime.lineDetailCalls.size)
        now++
        assertNotNull(service.alignSyntheticCandidateCached(leg, synthetic))
        assertEquals(listOf("forward"), realtime.lineDetailCalls)
    }

    @Test
    fun reverseGroup_preservesLegGeometry_walkingOffsets_andTimestamps() = runTest {
        val second = leg.copy(
            legIndex = 1, fromStopName = "丙站", toStopName = "丁站",
            fromStopId = "c", toStopId = "d", fromStopLat = 30.1, fromStopLng = 120.1,
            toStopLat = 30.2, toStopLng = 120.2, walkingDistanceMeters = 250,
            candidateLines = listOf(synthetic.copy(fromStopOrder = 2, toStopOrder = 8))
        )
        val group = TransitGroup("saved", "上班", sortOrder = 5, legs = listOf(leg, second))
        val service = RouteAlignmentService(
            settings, realtime, ioDispatcher = StandardTestDispatcher(testScheduler), wallClockMillis = { now }
        )

        val reversed = service.buildReverseTransitGroup(group)

        assertNotEquals(group.id, reversed.id)
        assertEquals("home", reversed.customGroupId)
        assertEquals(6, reversed.sortOrder)
        assertEquals(now, reversed.createdAt)
        assertEquals(now, reversed.updatedAt)
        assertEquals(listOf("丁站", "乙站"), reversed.legs.map { it.fromStopName })
        assertEquals(listOf("丙站", "甲站"), reversed.legs.map { it.toStopName })
        assertEquals(listOf(0, 250), reversed.legs.map { it.walkingDistanceMeters })
        assertEquals("d", reversed.legs.first().fromStopId)
        assertEquals("c", reversed.legs.first().toStopId)
        assertEquals(30.2, reversed.legs.first().fromStopLat)
        assertEquals(120.1, reversed.legs.first().toStopLng)
        assertEquals(1, reversed.legs.first().candidateLines.single().direction)
        assertEquals(7, reversed.legs.first().candidateLines.single().toStopOrder)
    }

    @Test
    fun legacyOrMissingCity_fallsBackToSelectedCity_butExplicitCityIsPreserved() {
        val service = RouteAlignmentService(settings, realtime)

        assertSame(city, service.resolveCandidateCity(synthetic, city))
        assertSame(city, service.resolveCandidateCity(synthetic.copy(cityId = "033"), city))
        assertEquals(TransitCity("0755", "深圳"), service.resolveCandidateCity(
            synthetic.copy(cityId = "0755", cityName = "深圳"), city
        ))
    }
}
