package com.trackr.core.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.trackr.core.navigation.scene.BottomSheetSceneStrategy
import kotlin.math.abs
import kotlin.math.roundToInt

/** 标准版的两个同级栈：页签共享一个可反向的透明度；栈内仍由 Nav3 管理页面、弹层和预测返回。 */
@Composable
fun TabNavigationDisplay(
    state: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stacks = state.decoratedStacks(entryProvider)
    val selected = state.topLevel
    val target = if (selected == state.start) 0f else 1f
    val blend = remember { Animatable(target) }
    val back = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = back,
        isBackEnabled = selected != state.start && state.currentStack.size == 1,
        onBackCompleted = onBack,
    )
    val preview = (back.transitionState as? InProgress)?.latestEvent?.progress
    // 刚点「我的」就拖返回时，手势接手当前画面，不先跳到完全不透明。
    val gestureStart = remember(preview != null) { if (preview != null) blend.value else 0f }
    LaunchedEffect(target, preview) {
        if (preview != null) {
            blend.snapTo(gestureStart * (1f - preview.coerceIn(0f, 1f)))
        } else {
            // 中途点回直接从已经画出的值反向，耗时与剩余距离成正比，不重新播整段或先跳到端点。
            val remaining = (TAB_FADE_MS * abs(target - blend.value)).roundToInt()
            if (remaining == 0) blend.snapTo(target)
            else blend.animateTo(target, tween(remaining, easing = LinearEasing))
        }
    }
    val showStart by remember { derivedStateOf { blend.value < 1f } }
    val showOther by remember { derivedStateOf { blend.value > 0f } }
    val atTarget by remember(target) { derivedStateOf { blend.value == target } }
    val strategies = remember { listOf(BottomSheetSceneStrategy<NavKey>()) }
    val tabs = remember { TopLevel.visibleTabs() }
    Box(modifier) {
        // 固定层序，永远只混合上层；到端点即移除非活动栈，后台页不继续组合或轮询。
        for (tab in tabs) key(tab) {
            val active = tab == selected
            val above = tab != state.start
            if (active || (if (above) showOther else showStart)) {
                val lifecycle = rememberLifecycleOwner(
                    maxLifecycle = if (active && atTarget) Lifecycle.State.RESUMED else Lifecycle.State.STARTED,
                )
                val dispatcher = rememberNavigationEventDispatcherOwner(enabled = active)
                CompositionLocalProvider(
                    LocalLifecycleOwner provides lifecycle,
                    LocalNavigationEventDispatcherOwner provides dispatcher,
                ) {
                    NavDisplay(
                        entries = stacks.getValue(tab), onBack = onBack, sceneStrategies = strategies,
                        modifier = Modifier.fillMaxSize()
                            .then(if (above) Modifier.graphicsLayer { alpha = blend.value } else Modifier)
                            .then(if (active) Modifier else Modifier.semantics { hideFromAccessibility() }),
                        transitionSpec = TrackrTransitions.push, popTransitionSpec = TrackrTransitions.pop,
                        predictivePopTransitionSpec = TrackrTransitions.predictivePop,
                    )
                }
            }
        }
    }
}

private const val TAB_FADE_MS = 216
