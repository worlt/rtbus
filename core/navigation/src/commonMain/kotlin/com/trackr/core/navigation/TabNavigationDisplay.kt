package com.trackr.core.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.NavigationBackHandler as SceneNavigationBackHandler
import androidx.navigation3.scene.rememberNavigationEventState as rememberSceneNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.trackr.core.navigation.scene.BottomSheetSceneStrategy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

/** 同级页使用原生 Pager，保留两页组合；子页、弹层和预测返回仍由各自的 Nav3 栈管理。 */
@Composable
fun TabNavigationDisplay(
    state: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectTab: (ScreenKey) -> Unit,
) {
    val stacks = state.decoratedStacks(entryProvider)
    val tabs = remember { TopLevel.visibleTabs() }
    val selected = state.topLevel
    val target = tabs.indexOf(selected).coerceAtLeast(0)
    val start = tabs.indexOf(state.start)
    val pager = rememberPagerState(initialPage = target, pageCount = { tabs.size })
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val selectTab by rememberUpdatedState(onSelectTab)
    val back = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = back,
        isBackEnabled = selected != state.start && state.currentStack.size == 1,
        onBackCompleted = onBack,
    )
    val preview = (back.transitionState as? InProgress)?.latestEvent?.progress
    val gestureStart = remember(preview != null) { pager.currentPage + pager.currentPageOffsetFraction }
    LaunchedEffect(target, preview) {
        if (preview != null) {
            val position = gestureStart + (start - gestureStart) * preview.coerceIn(0f, 1f)
            val page = position.roundToInt().coerceIn(tabs.indices)
            pager.scrollToPage(page, position - page)
        } else {
            val distance = abs(target - pager.currentPage - pager.currentPageOffsetFraction)
            if (distance > 0f) {
                pager.animateScrollToPage(target, animationSpec = tween(
                    (TAB_SLIDE_MS * distance).roundToInt().coerceAtLeast(1), easing = tabEasing,
                ))
            }
        }
    }
    // 只有用户拖动结束才提交页签；程序动画、预测返回预览不能抢回用户刚点的目标。
    LaunchedEffect(pager) {
        snapshotFlow { dragged }.filter { it }.collectLatest {
            val requested = state.topLevel
            snapshotFlow { pager.isScrollInProgress }.first { !it }
            if (back.transitionState !is InProgress && state.topLevel == requested && state.currentStack.size == 1) {
                val tab = tabs[pager.settledPage]
                if (tab != state.topLevel) selectTab(tab)
            }
        }
    }
    val idle by remember { derivedStateOf { !pager.isScrollInProgress && pager.currentPageOffsetFraction == 0f } }
    val strategies = remember { listOf(BottomSheetSceneStrategy<NavKey>()) }
    HorizontalPager(
        state = pager,
        modifier = modifier,
        key = { tabs[it].toString() },
        beyondViewportPageCount = 1,
        // 子页和弹层不参与同级横滑，避免抢走详情手势或跨栈离开弹窗。
        userScrollEnabled = state.currentStack.size == 1 && preview == null,
    ) { page ->
        val tab = tabs[page]
        val active = tab == selected
        val lifecycle = rememberLifecycleOwner(maxLifecycle = when {
            active && idle && pager.settledPage == target -> Lifecycle.State.RESUMED
            active || !idle -> Lifecycle.State.STARTED
            else -> Lifecycle.State.CREATED
        })
        val dispatcher = rememberNavigationEventDispatcherOwner(enabled = active)
        CompositionLocalProvider(LocalLifecycleOwner provides lifecycle) {
            val scenes = rememberSceneState(stacks.getValue(tab), sceneStrategies = strategies, onBack = onBack)
            val events = rememberSceneNavigationEventState(scenes)
            // Gate only this stack's handler. Native dialogs must resolve their own window owner.
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcher) {
                SceneNavigationBackHandler(scenes, events, onBackCompleted = onBack)
            }
            NavDisplay(
                sceneState = scenes, navigationEventState = events,
                modifier = Modifier.fillMaxSize()
                    .then(if (active || !idle) Modifier else Modifier.clearAndSetSemantics {}),
                transitionSpec = TrackrTransitions.push, popTransitionSpec = TrackrTransitions.pop,
                predictivePopTransitionSpec = TrackrTransitions.predictivePop,
            )
        }
    }
}

private const val TAB_SLIDE_MS = 280
private val tabEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
