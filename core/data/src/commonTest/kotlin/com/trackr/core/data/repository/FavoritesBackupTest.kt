package com.trackr.core.data.repository

import com.trackr.core.data.local.FavoritesSnapshot
import com.trackr.core.data.local.FavoritesStore
import com.trackr.core.data.local.FavoritesWrite
import com.trackr.core.model.*
import com.trackr.core.testing.InMemoryFavoritesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FavoritesBackupTest {
    private var counter = 0
    private suspend fun open(store: FavoritesStore = InMemoryFavoritesStore()) =
        DefaultFavoritesRepository.create(store, Dispatchers.Unconfined, newId = { "new_${++counter}" })

    private fun stop(id: String = "s1", city: String = "051") = FavoriteStationLine(
        id = id, cityId = city, cityName = "城市$city", lineId = "line", lineName = "1路",
        startStop = "起点", endStop = "终点", targetStopId = "station", targetStopName = "上车站", targetOrder = 3
    )

    @Test fun roundTripPreservesEveryKindGroupColorAndOrder() = runTest {
        val source = open()
        val group = source.createGroup("出差", GroupColor.Preset.BLUE)
        source.toggleFavorite(stop())
        source.toggleStation(FavoriteStation("station", "034", "上海", "人民广场", "st"))
        source.saveRoute(TransitGroup("route", "回家", legs = listOf(TransitLeg(0, "起点", toStopName = "终点"))))
        source.favoriteItems.value.forEach { source.setGroup(it.key, group.id) }
        val keys = source.favoriteItems.value.map { it.key }.reversed()
        source.move(keys)
        val store = InMemoryFavoritesStore()
        val target = open(store)
        assertEquals(3, target.importFavorites(source.exportFavorites()).added)
        assertEquals(source.favoriteItems.value.map { it::class }, target.favoriteItems.value.map { it::class })
        assertEquals(source.favorites.value, target.favorites.value)
        assertEquals(source.favoriteStations.value, target.favoriteStations.value)
        assertEquals(source.routes.value.first().legs, target.routes.value.first().legs)
        val importedGroup = target.favoriteGroups.value.single { it.name == "出差" }
        assertEquals(group.colorKey, importedGroup.colorKey)
        assertTrue(target.favoriteItems.value.all { it.groupId == importedGroup.id })
        val reopened = open(store)
        assertEquals(target.favoriteItems.value, reopened.favoriteItems.value)
        assertEquals(target.favoriteGroups.value, reopened.favoriteGroups.value)
    }

    @Test fun sameNameGroupsRemainDistinctAndRepeatedImportsReuseEachGroup() = runTest {
        val source = open()
        val blue = source.createGroup("通勤", GroupColor.Preset.BLUE)
        val another = source.createGroup("通勤")
        source.toggleFavorite(stop("blue", "034"))
        source.toggleFavorite(stop("another", "001"))
        source.setGroup(FavoriteItem.stopKey("blue"), blue.id)
        source.setGroup(FavoriteItem.stopKey("another"), another.id)
        val text = source.exportFavorites()
        val target = open()
        val first = target.importFavorites(text)
        assertEquals(2, first.groupsAdded)
        assertEquals(2, first.added)
        val restored = target.favoriteGroups.value.filter { it.name == "通勤" }
        assertEquals(2, restored.size)
        assertEquals(listOf(blue.colorKey, another.colorKey), restored.map { it.colorKey })
        assertEquals(2, target.favoriteItems.value.map { it.groupId }.distinct().size)
        val groups = target.favoriteGroups.value
        val items = target.favoriteItems.value
        assertEquals(FavoritesImportResult(0, 2, 0), target.importFavorites(text))
        assertEquals(groups, target.favoriteGroups.value)
        assertEquals(items, target.favoriteItems.value)
    }

    @Test fun mergeKeepsExistingDataAndRepeatedImportsDoNotDuplicate() = runTest {
        val source = open(); source.toggleFavorite(stop())
        val target = open(); target.toggleFavorite(stop("same-content-other-id"))
        target.toggleFavorite(stop("other-city", "001"))
        val before = target.favoriteItems.value
        val text = source.exportFavorites()
        assertEquals(0, target.importFavorites(text).added)
        assertEquals(0, target.importFavorites(text).added)
        assertEquals(before, target.favoriteItems.value)
    }

    @Test fun idCollisionsAreRemappedAndNewItemsAppendInBackupOrder() = runTest {
        val source = open(); val group = source.createGroup("新组")
        source.toggleFavorite(stop("collision", "034")); source.setGroup(FavoriteItem.stopKey("collision"), group.id)
        val target = open(); target.createGroup("已有组"); target.toggleFavorite(stop("collision", "001"))
        val before = target.favoriteItems.value
        assertEquals(1, target.importFavorites(source.exportFavorites()).added)
        assertEquals(before.first(), target.favoriteItems.value.first())
        assertEquals(2, target.favorites.value.map { it.id }.distinct().size)
        assertEquals("新组", target.favoriteGroups.value.single { it.id == target.favoriteItems.value.last().groupId }.name)
        assertEquals(0, target.importFavorites(source.exportFavorites()).added)
    }

    @Test fun malformedUnsupportedOversizedAndBrokenReferencesNeverWrite() = runTest {
        val source = open();source.toggleFavorite(stop());val good = source.exportFavorites()
        val store = InMemoryFavoritesStore();val target = open(store);target.toggleFavorite(stop("keep", "001"))
        val before = target.favoriteItems.value;val writes = store.writes.size
        for (bad in listOf("not json", "{}", good.replace("rtbus-favorites", "other-app"),
            good.replace("\"version\": 1", "\"version\": 999"),
            good.replace("\"refId\": \"s1\"", "\"refId\": \"missing\""), "x".repeat(8 * 1024 * 1024 + 1))) {
            assertFailsWith<Exception> { target.importFavorites(bad) }
            assertEquals(before, target.favoriteItems.value)
            assertEquals(writes, store.writes.size)
        }
    }

    @Test fun concurrentImportsAreSerializedAndDoNotDuplicate() = runTest {
        val source = open();source.toggleFavorite(stop());val text = source.exportFavorites();val target = open()
        val results = List(4) { async { target.importFavorites(text) } }.awaitAll()
        assertEquals(1, results.sumOf { it.added });assertEquals(1, target.favoriteItems.value.size)
    }

    @Test fun writeFailureDoesNotPublishSuccessfulImport() = runTest {
        val source = open();source.toggleFavorite(stop());val text = source.exportFavorites()
        var fail = false
        val store = object : FavoritesStore {
            val delegate = InMemoryFavoritesStore()
            override suspend fun load() = delegate.load()
            override suspend fun write(change: FavoritesWrite) { if (fail) error("disk full");delegate.write(change) }
        }
        val target = open(store);fail = true
        assertFailsWith<IllegalStateException> { target.importFavorites(text) }
        assertTrue(target.favoriteItems.value.isEmpty())
    }

    @Test fun emptyBackupIsANoOpAndTemporaryRoutesAreNotExported() = runTest {
        val source = open();source.saveRoute(TransitGroup("temporary", "临时", isTemporary = true))
        val store = InMemoryFavoritesStore();val target = open(store);val before = store.writes.size
        assertEquals(0, target.importFavorites(source.exportFavorites()).added)
        assertTrue(target.routes.value.isEmpty());assertEquals(before, store.writes.size)
    }
    @Test fun importingSavedRouteDoesNotOverwriteALocalTemporaryRouteWithTheSameId() = runTest {
        val source = open();source.saveRoute(TransitGroup("route", "已保存路线"))
        val target = open();target.saveRoute(TransitGroup("route", "本机临时路线", isTemporary = true))
        assertEquals(1, target.importFavorites(source.exportFavorites()).added)
        assertEquals(2, target.routes.value.size)
        assertEquals(2, target.routes.value.map { it.id }.distinct().size)
        assertTrue(target.routes.value.any { it.id == "route" && it.isTemporary })
    }

}
