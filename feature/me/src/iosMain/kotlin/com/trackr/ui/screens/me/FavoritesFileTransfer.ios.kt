package com.trackr.ui.screens.me

import androidx.compose.runtime.Composable
import com.trackr.core.data.repository.FavoritesBackup

/** 当前文件选择交互在 Android 提供；iOS 隐藏入口，保留共享备份格式。 */
@Composable
actual fun rememberFavoritesFileTransfer(backup: FavoritesBackup, onMessage: (String) -> Unit): FavoritesFileTransfer? = null
