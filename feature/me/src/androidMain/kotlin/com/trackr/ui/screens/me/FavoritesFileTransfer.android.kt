package com.trackr.ui.screens.me

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.trackr.core.data.repository.FavoritesBackup
import com.trackr.core.data.repository.MAX_FAVORITES_BACKUP_BYTES
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate

@Composable
actual fun rememberFavoritesFileTransfer(backup: FavoritesBackup, onMessage: (String) -> Unit): FavoritesFileTransfer? {
    val context = LocalContext.current
    val currentBackup by rememberUpdatedState(backup)
    val message by rememberUpdatedState(onMessage)
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) {
            busy = false
        } else {
            busy = true
            scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val bytes = currentBackup.exportFavorites().toByteArray(Charsets.UTF_8)
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) { "无法打开保存位置" }.use { it.write(bytes) }
                }
                message("收藏已导出到所选文件")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message("导出失败，请检查保存位置和可用空间后重试")
            } finally { busy = false }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            busy = false
        } else {
            busy = true
            scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val text = requireNotNull(context.contentResolver.openInputStream(uri)) { "无法读取文件" }
                        .use { readFavoritesBackup(it) }
                    currentBackup.importFavorites(text)
                }
                message("已导入 ${result.added} 条收藏，跳过 ${result.skipped} 条重复，新增 ${result.groupsAdded} 个分组")
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message("导入失败：请使用有效的 RTBus 收藏 JSON 备份（不超过 8 MB），并检查存储空间")
            } finally { busy = false }
            }
        }
    }
    return FavoritesFileTransfer(busy, exportFavorites = {
        if (!busy) {
            busy = true
            try { exportLauncher.launch("RTBus-favorites-${LocalDate.now()}.json") }
            catch (_: Exception) { busy = false; message("无法打开系统文件选择器") }
        }
    }, importFavorites = {
        if (!busy) {
            busy = true
            try { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            catch (_: Exception) { busy = false; message("无法打开系统文件选择器") }
        }
    })
}

/** 有界读取，包括无法提前提供文件大小的云盘；不申请存储目录权限。 */
internal fun readFavoritesBackup(input: InputStream): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(output.size() + count <= MAX_FAVORITES_BACKUP_BYTES) { "收藏文件超过 8 MB" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray().toString(Charsets.UTF_8).removePrefix("\uFEFF")
}
