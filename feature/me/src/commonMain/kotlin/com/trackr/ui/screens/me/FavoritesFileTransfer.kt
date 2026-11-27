package com.trackr.ui.screens.me

import androidx.compose.runtime.Composable
import com.trackr.core.data.repository.FavoritesBackup

/** 平台文件选择器，只在用户主动导入 / 导出时访问所选文件。 */
class FavoritesFileTransfer(
    val isBusy: Boolean,
    val exportFavorites: () -> Unit,
    val importFavorites: () -> Unit
)

@Composable
expect fun rememberFavoritesFileTransfer(
    backup: FavoritesBackup,
    onMessage: (String) -> Unit
): FavoritesFileTransfer?
