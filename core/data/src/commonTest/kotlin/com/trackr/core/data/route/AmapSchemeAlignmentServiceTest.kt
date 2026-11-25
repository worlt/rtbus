package com.trackr.core.data.route

import com.trackr.core.data.provider.FakeTestTransitProvider
import com.trackr.core.data.provider.TransitProviderCoordinator
import com.trackr.core.model.AmapTransitSchemeResult
import com.trackr.core.model.BusLineDetail
import com.trackr.core.model.BusLineDirection
import com.trackr.core.model.BusLineSummary
import com.trackr.core.model.BusStop
import com.trackr.core.model.CandidateLine
import com.trackr.core.model.DataSourceType
import com.trackr.core.model.TransitCity
import com.trackr.core.model.TransitGroup
import com.trackr.core.model.TransitLeg
import com.trackr.core.model.TransitSearchResult
import com.trackr.core.testing.FakeRealtimeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AmapSchemeAlignmentServiceTest {
    private val city = TransitCity("034", "上海", mygolbsCityCode = "shanghai")
    private val realtime = FakeRealtimeRepository()
    private val provider = FakeTestTransitProvider(DataSourceType.CHELAILE)
    private val unusedProvider = FakeTestTransitProvider(DataSourceType.MYGOLBS)
    private val coordinator = TransitProviderCoordinator(provider, unusedProvider)

    private fun direction(id: String = "real", name: String = "1路", direction: Int = 0) = BusLineDirection(
        lineId = id, name = name, direction = direction, startStop = "首站", endStop = "末站"
    )

    private fun searchResult(vararg directions: BusLineDirection) = TransitSearchResult(
        keyword = "1路",
        lines = directions.map { BusLineSummary(it.name, it.name, directions = listOf(it)) }
    )

    private fun scheme(name: String = "1路", from: String = "甲站", to: String = "乙站") = AmapTransitSchemeResult(
        title = name, durationMinutes = 20, walkingMeters = 100, costYuan = "2", transferCount = 0,
        group = TransitGroup(
            id = "scheme_$name", name = name,
            legs = listOf(TransitLeg(
                legIndex = 0, fromStopName = from, toStopName = to,
                candidateLines = listOf(CandidateLine(
                    cityId = "old", cityName = "旧城市", lineId = "line_synthetic", lineName = name,
                    fromStopOrder = 2, toStopOrder = 8, startStopName = from, endStopName = to
                ))
            ))
        )
    )

    private fun AmapTransitSchemeResult.candidate() = group.legs.single().candidateLines.single()

    @Test
    fun noCoordinator_returnsOriginalSchemesWithoutQueries() = runTest {
        val input = listOf(scheme())
        val service = AmapSchemeAlignmentService(realtime, ioDispatcher = StandardTestDispatcher(testScheduler))

        assertSame(input, service.alignAmapSchemesWithRealtimeSource(input, city))
        assertTrue(realtime.lineDetailCalls.isEmpty())
    }

    @Test
    fun visibleSchemes_deduplicateNormalizedStops_andPreserveHiddenSchemes() = runTest {
        provider.searchResult = searchResult(direction())
        val input = listOf(scheme(), scheme(from = "甲站(东)", to = "乙站(西)"), scheme(), scheme(), scheme("99路"))
        val service = AmapSchemeAlignmentService(realtime, coordinator, StandardTestDispatcher(testScheduler))

        val output = service.alignAmapSchemesWithRealtimeSource(input, city)

        assertEquals(5, output.size)
        assertEquals(1, provider.searchCallCount)
        assertEquals(listOf("real"), realtime.lineDetailCalls)
        assertTrue(output.take(4).all { it.candidate().lineId == "real" })
        assertSame(input.last(), output.last())
        assertEquals(0, unusedProvider.searchCallCount)
        assertEquals(0, provider.lineDetailCallCount, "详情只走 realtime，不重复查 Provider")
    }

    @Test
    fun orderedStops_chooseMatchingDirection_andPreserveSchemeMetadata() = runTest {
        val wrong = direction("wrong")
        val right = direction("right", direction = 1)
        provider.searchResult = searchResult(wrong, right)
        realtime.lineDetailsMap[wrong.lineId] = BusLineDetail(line = wrong, stops = listOf(
            BusStop("b", "乙站", 3), BusStop("a", "甲站", 7)
        ), buses = emptyList())
        realtime.lineDetailsMap[right.lineId] = BusLineDetail(line = right, stops = listOf(
            BusStop("a", "甲站(东)", 4), BusStop("b", "乙站(西)", 9)
        ), buses = emptyList())
        val input = scheme()
        val expected = input.candidate().copy(
            cityId = city.cityId, cityName = city.cityName, mygolbsCityCode = city.mygolbsCityCode,
            lineId = "right", direction = 1, fromStopOrder = 4, toStopOrder = 9,
            startStopName = "甲站(东)", endStopName = "乙站(西)"
        )
        val service = AmapSchemeAlignmentService(realtime, coordinator, StandardTestDispatcher(testScheduler))

        val output = service.alignAmapSchemesWithRealtimeSource(listOf(input), city).single()

        assertEquals(expected, output.candidate())
        assertEquals(input, output.copy(group = input.group), "规划元数据不变")
        assertEquals(input.group.legs.single(), output.group.legs.single().copy(candidateLines = input.group.legs.single().candidateLines))
        assertEquals(listOf("wrong", "right"), realtime.lineDetailCalls)
    }

    @Test
    fun missingDetails_fallsBackToFirstDirection_withoutReplacingStopOrders() = runTest {
        provider.searchResult = searchResult(direction("first", direction = 1), direction("second"))
        realtime.lineDetailResult = Result.failure(IllegalStateException("offline"))
        val input = scheme()
        val service = AmapSchemeAlignmentService(realtime, coordinator, StandardTestDispatcher(testScheduler))

        val output = service.alignAmapSchemesWithRealtimeSource(listOf(input), city).single()

        assertEquals(input.candidate().copy(
            cityId = city.cityId, cityName = city.cityName, mygolbsCityCode = city.mygolbsCityCode,
            lineId = "first", direction = 1
        ), output.candidate())
        assertEquals(listOf("first", "second"), realtime.lineDetailCalls)
    }

    @Test
    fun unmatchedLine_preservesSyntheticCandidate() = runTest {
        provider.searchResult = searchResult(direction(name = "99路"))
        val input = listOf(scheme())
        val service = AmapSchemeAlignmentService(realtime, coordinator, StandardTestDispatcher(testScheduler))

        assertEquals(input, service.alignAmapSchemesWithRealtimeSource(input, city))
        assertTrue(realtime.lineDetailCalls.isEmpty())
    }

    @Test
    fun searchFailure_preservesSyntheticCandidate() = runTest {
        provider.searchFailure = IllegalStateException("offline")
        val input = listOf(scheme())
        val service = AmapSchemeAlignmentService(realtime, coordinator, StandardTestDispatcher(testScheduler))

        assertEquals(input, service.alignAmapSchemesWithRealtimeSource(input, city))
        assertEquals(1, provider.searchCallCount)
        assertTrue(realtime.lineDetailCalls.isEmpty())
    }

    @Test
    fun distinctCandidates_limitConcurrentQueriesToFour() = runTest {
        provider.searchResult = searchResult(*(1..8).map { direction("real_$it", "${it}路") }.toTypedArray())
        val base = scheme()
        val input = listOf(base.copy(group = base.group.copy(legs = listOf(base.group.legs.single().copy(
            candidateLines = (1..8).map { base.candidate().copy(lineName = "${it}路") }
        )))))
        var active = 0
        var peak = 0
        realtime.lineDetailHook = {
            active++
            peak = maxOf(peak, active)
            try { delay(100) } finally { active-- }
        }
        val service = AmapSchemeAlignmentService(realtime, coordinator, StandardTestDispatcher(testScheduler))

        val output = service.alignAmapSchemesWithRealtimeSource(input, city)

        assertEquals(4, peak)
        assertEquals(0, active)
        assertEquals(8, provider.searchCallCount)
        assertEquals((1..8).map { "real_$it" }, output.single().group.legs.single().candidateLines.map { it.lineId })
    }

    @Test
    fun cancellation_cleansUpInFlightQueries_withoutReturningPartialSchemes() = runTest {
        provider.searchResult = searchResult(direction())
        var active = 0
        realtime.lineDetailHook = {
            active++
            try { awaitCancellation() } finally { active-- }
        }
        val service = AmapSchemeAlignmentService(realtime, coordinator, StandardTestDispatcher(testScheduler))
        val result = async { service.alignAmapSchemesWithRealtimeSource(listOf(scheme()), city) }
        runCurrent()
        assertEquals(1, active)

        result.cancelAndJoin()

        assertFailsWith<CancellationException> { result.await() }
        assertEquals(0, active)
        assertEquals(listOf("real"), realtime.lineDetailCalls)
    }
}
