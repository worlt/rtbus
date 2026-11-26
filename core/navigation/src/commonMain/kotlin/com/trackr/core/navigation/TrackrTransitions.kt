package com.trackr.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

object TrackrTransitions {
    private const val PAGE_MILLIS = 304
    // Short rounded ends and a broad cruise section; one trajectory for both edges of a push/pop.
    private val pageEasing = CubicBezierEasing(0.25f, 0.08f, 0.75f, 0.92f)
    private val pageSlide = tween<IntOffset>(PAGE_MILLIS, easing = pageEasing)

    val push: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        slideInHorizontally(pageSlide) { it } togetherWith slideOutHorizontally(pageSlide) { -it / 5 }
    }

    val pop: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = { revealPrevious(1, pageSlide) }
    private val gestureSlide = tween<IntOffset>(PAGE_MILLIS, easing = LinearEasing)
    val predictivePop: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { edge ->
        revealPrevious(if (edge == NavigationEvent.EDGE_RIGHT) -1 else 1, gestureSlide)
    }

    private fun AnimatedContentTransitionScope<Scene<NavKey>>.revealPrevious(
        direction: Int, slide: TweenSpec<IntOffset>,
    ): ContentTransform =
        slideInHorizontally(slide) { -direction * it / 5 } togetherWith slideOutHorizontally(slide) { direction * it }
}
