package com.trackr.feature.bus.home

import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.lifecycle.Lifecycle

import androidx.compose.runtime.getValue

import androidx.compose.runtime.collectAsState

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.trackr.core.designsystem.resources.Res
import com.trackr.core.designsystem.resources.action_cancel
import com.trackr.core.designsystem.resources.action_confirm
import com.trackr.feature.bus.resources.Res as BusRes
import com.trackr.feature.bus.resources.bus_city_change_title
import com.trackr.feature.bus.resources.bus_city_change_message
import org.jetbrains.compose.resources.stringResource

/** 标准版与长辈版共用，返回或点弹窗外等同取消，不修改跨城偏好。 */
@Composable
fun CityChangeConfirmation(state: BusHomeUiState, onEvent: (BusHomeEvent) -> Unit) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (lifecycleState != Lifecycle.State.RESUMED) return
    val candidate = state.pendingCityChange ?: return
    AlertDialog(
        onDismissRequest = { onEvent(BusHomeEvent.CancelCityChange) },
        title = { Text(stringResource(BusRes.string.bus_city_change_title)) },
        text = { Text(stringResource(BusRes.string.bus_city_change_message, candidate.cityName, state.currentCity.cityName)) },
        confirmButton = {
            TextButton(onClick = { onEvent(BusHomeEvent.ConfirmCityChange) }) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(BusHomeEvent.CancelCityChange) }) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
