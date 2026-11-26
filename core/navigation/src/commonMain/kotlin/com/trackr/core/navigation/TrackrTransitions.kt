package com.trackr.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene

/** 同级页签原位叠化；进子页只移动前景，底下的页始终铺满，不会在两页之间露出窗口背景。 */
object TrackrTransitions {
    private const val TAB_MILLIS = 160
    const val PAGE_MILLIS = 220

    private object TabRoot : NavMetadataKey<Boolean>
    val tabRootMetadata: Map<String, Any> = metadata { put(TabRoot, true) }

    private fun AnimatedContentTransitionScope<Scene<NavKey>>.betweenTabs() =
        initialState.metadata[TabRoot] == true && targetState.metadata[TabRoot] == true

    val push: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        val enter = if (betweenTabs()) {
            fadeIn(tween(TAB_MILLIS))
        } else {
            slideInHorizontally(tween(PAGE_MILLIS)) { it / 12 } + fadeIn(tween(PAGE_MILLIS))
        }
        enter togetherWith ExitTransition.KeepUntilTransitionsFinished
    }

    val pop: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = { revealPrevious() }
    val predictivePop: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { revealPrevious() }

    private fun AnimatedContentTransitionScope<Scene<NavKey>>.revealPrevious(): ContentTransform {
        val exit = if (betweenTabs()) {
            fadeOut(tween(TAB_MILLIS))
        } else {
            slideOutHorizontally(tween(PAGE_MILLIS)) { it / 12 } + fadeOut(tween(PAGE_MILLIS))
        }
        return EnterTransition.None togetherWith exit
    }
}
