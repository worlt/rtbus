package com.trackr.core.data.repository

import com.trackr.core.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 可移植文件的入口；导入始终合并，保留现有收藏。 */
interface FavoritesBackup {
    suspend fun exportFavorites(): String
    suspend fun importFavorites(text: String): FavoritesImportResult
}

data class FavoritesImportResult(val added: Int, val skipped: Int, val groupsAdded: Int)

const val MAX_FAVORITES_BACKUP_BYTES = 8 * 1024 * 1024

internal object FavoritesBackupCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private const val FORMAT = "rtbus-favorites"
    private const val VERSION = 1

    fun archive(stops: List<FavoriteStationLine>, routes: List<TransitGroup>, stations: List<FavoriteStation>,
                groups: List<FavoriteGroup>, items: List<FavoriteItemRecord>, includeTemporary: Boolean = false): FavoritesArchive {
        val savedRoutes = if (includeTemporary) routes else routes.filterNot { it.isTemporary }
        val temporaryKeys = if (includeTemporary) emptySet() else routes.filter { it.isTemporary }.map { FavoriteItem.routeKey(it.id) }.toSet()
        return FavoritesArchive(FORMAT, VERSION, stops, stations, savedRoutes, groups, items.filterNot { it.key in temporaryKeys })
    }

    fun encode(archive: FavoritesArchive): String = json.encodeToString(archive).also {
        require(it.encodeToByteArray().size <= MAX_FAVORITES_BACKUP_BYTES) { "收藏文件超过 8 MB，请减少收藏后重试" }
    }

    fun decode(text: String): FavoritesArchive {
        require(text.length <= MAX_FAVORITES_BACKUP_BYTES) { "收藏文件超过 8 MB" }
        val archive = json.decodeFromString<FavoritesArchive>(text)
        require(archive.format == FORMAT && archive.version == VERSION) { "不是受支持的 RTBus 收藏备份" }
        val count = archive.stops.size + archive.stations.size + archive.routes.size + archive.groups.size
        require(count <= 10_000 && archive.items.size <= 10_000) { "收藏文件条目过多" }
        fun unique(ids: List<String>) = ids.all { it.isNotBlank() } && ids.distinct().size == ids.size
        require(unique(archive.stops.map { it.id }) && unique(archive.stations.map { it.id }) &&
            unique(archive.routes.map { it.id }) && unique(archive.groups.map { it.id })) { "收藏文件有重复或空白编号" }
        require(archive.stops.all { it.cityId.isNotBlank() && it.lineId.isNotBlank() && it.targetOrder >= 0 }) { "收藏线路信息不完整" }
        require(archive.stations.all { it.cityId.isNotBlank() && it.stopName.isNotBlank() }) { "收藏站点信息不完整" }
        require(archive.routes.none { it.isTemporary } && archive.routes.all { it.legs.size <= 100 }) { "收藏路线信息无效" }
        require(archive.groups.all { it.name.isNotBlank() }) { "收藏分组名称为空" }
        val keys = (archive.stops.map { FavoriteItem.stopKey(it.id) } +
            archive.stations.map { FavoriteItem.stationKey(it.id) } + archive.routes.map { FavoriteItem.routeKey(it.id) }).toSet()
        val groupIds = archive.groups.map { it.id }.toSet()
        require(archive.items.map { it.key }.toSet() == keys && archive.items.size == keys.size &&
            archive.items.all { it.groupId == null || it.groupId in groupIds }) { "收藏文件的排序或分组引用不完整" }
        return archive
    }

    data class Merge(val archive: FavoritesArchive, val result: FavoritesImportResult)

    /** 在仓库锁内基于最新数据合并；编号冲突改名、语义相同去重，既有内容和顺序不动。 */
    fun merge(current: FavoritesArchive, incoming: FavoritesArchive, newId: () -> String): Merge {
        val stops = current.stops.toMutableList()
        val stations = current.stations.toMutableList()
        val routes = current.routes.toMutableList()
        val groups = current.groups.toMutableList()
        val records = current.items.toMutableList()
        val groupMap = mutableMapOf<String, String>()
        fun unused(wanted: String, ids: Set<String>): String {
            if (wanted !in ids) return wanted
            // 不依赖外部 id 生成器一定返回唯一值；重复编号也能安全合并。
            val base = newId(); var candidate = base; var suffix = 1
            while (candidate in ids) candidate = "${base}_${suffix++}"
            return candidate
        }
        for (group in incoming.groups.sortedBy { it.sortOrder }) {
            val existing = groups.firstOrNull { it.name.trim() == group.name.trim() }
            val id = existing?.id ?: unused(group.id, groups.map { it.id }.toSet())
            if (existing == null) groups += group.copy(id = id, sortOrder = (groups.maxOfOrNull { it.sortOrder } ?: -1) + 1)
            groupMap[group.id] = id
        }
        fun FavoriteStationLine.identity() = listOf(cityId, source.name, lineId, direction.toString(),
            targetStopId.ifBlank { "order:$targetOrder" })
        fun FavoriteStation.identity() = listOf(cityId, source.name, namesakeStId.ifBlank { stopName.trim() })
        fun TransitGroup.content() = copy(id = "", sortOrder = 0, customGroupId = null, createdAt = 0L, updatedAt = 0L)
        val stopIdentities = stops.map { it.identity() }.toMutableSet()
        val stationIdentities = stations.map { it.identity() }.toMutableSet()
        val routeContents = routes.map { it.content() }.toMutableSet()
        val stopIds = stops.map { it.id }.toMutableSet()
        val stationIds = stations.map { it.id }.toMutableSet()
        val routeIds = routes.map { it.id }.toMutableSet()
        val incomingStops = incoming.stops.associateBy { it.id }
        val incomingStations = incoming.stations.associateBy { it.id }
        val incomingRoutes = incoming.routes.associateBy { it.id }
        var added = 0
        for (record in incoming.items.sortedBy { it.sortOrder }) {
            val groupId = record.groupId?.let { groupMap.getValue(it) }
            val nextOrder = (records.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val id = when (record.type) {
                FavoriteItemType.STOP -> {
                    val item = incomingStops.getValue(record.refId)
                    if (!stopIdentities.add(item.identity())) continue
                    val id = unused(item.id, stopIds).also { stopIds += it }
                    stops += item.copy(id = id); id
                }
                FavoriteItemType.STATION -> {
                    val item = incomingStations.getValue(record.refId)
                    if (!stationIdentities.add(item.identity())) continue
                    val id = unused(item.id, stationIds).also { stationIds += it }
                    stations += item.copy(id = id); id
                }
                FavoriteItemType.ROUTE -> {
                    val item = incomingRoutes.getValue(record.refId)
                    if (!routeContents.add(item.content())) continue
                    val id = unused(item.id, routeIds).also { routeIds += it }
                    routes += item.copy(id = id, customGroupId = groupId, sortOrder = (routes.maxOfOrNull { it.sortOrder } ?: -1) + 1); id
                }
            }
            records += record.copy(refId = id, groupId = groupId, sortOrder = nextOrder)
            added++
        }
        return Merge(current.copy(stops = stops, stations = stations, routes = routes, groups = groups, items = records),
            FavoritesImportResult(added, incoming.items.size - added, groups.size - current.groups.size))
    }
}
