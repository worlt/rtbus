package com.trackr.core.model

import kotlinx.serialization.Serializable

/** 可移植收藏文件；只含用户收藏和分组，不含定位、账号、提醒或应用设置。 */
@Serializable
data class FavoritesArchive(
    val format: String,
    val version: Int,
    val stops: List<FavoriteStationLine> = emptyList(),
    val stations: List<FavoriteStation> = emptyList(),
    val routes: List<TransitGroup> = emptyList(),
    val groups: List<FavoriteGroup> = emptyList(),
    val items: List<FavoriteItemRecord> = emptyList()
)
