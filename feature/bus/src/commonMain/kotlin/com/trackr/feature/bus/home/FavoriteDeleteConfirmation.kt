package com.trackr.feature.bus.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.trackr.core.designsystem.resources.Res
import com.trackr.core.designsystem.resources.action_cancel
import com.trackr.core.designsystem.resources.action_delete
import com.trackr.feature.bus.resources.Res as BusRes
import com.trackr.feature.bus.resources.bus_favorite_delete_title
import com.trackr.feature.bus.resources.bus_favorite_delete_body
import org.jetbrains.compose.resources.stringResource

/** 标准版和长辈版共用：长按只询问，取消不操作，确认才发出精确到 key 的删除事件。 */
@Composable
internal fun FavoriteDeleteConfirmation(
    row: FavoriteRowUi,
    onEvent: (BusHomeEvent) -> Unit,
    content: @Composable (onLongClick: () -> Unit) -> Unit
) {
    var asking by rememberSaveable(row.key) { mutableStateOf(false) }
    content { asking = true }
    if (!asking) return
    val title = when (row) {
        is FavoriteRowUi.Stop -> "${row.lineName} · ${row.boardText} ${row.direction}"
        is FavoriteRowUi.Station -> row.title
        is FavoriteRowUi.Route -> row.title
    }
    AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text(stringResource(BusRes.string.bus_favorite_delete_title)) },
        text = { Text(stringResource(BusRes.string.bus_favorite_delete_body, title)) },
        confirmButton = {
            TextButton(onClick = {
                asking = false
                onEvent(BusHomeEvent.DeleteFavorite(row.key))
            }) { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = { asking = false }) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}
