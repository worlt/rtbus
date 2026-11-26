package com.trackr.core.navigation

import android.graphics.Color.red
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Sample rendered output, not just the final stack or bounds of two possibly transparent nodes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NavigationMotionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var nav: Navigator

    private fun launch() {
        rule.setContent {
            val state = rememberNavigationState(topLevel = TopLevel.visibleTabs())
            nav = remember(state) { Navigator(state) }
            TabNavigationDisplay(state = state, onBack = { nav.back() },
                modifier = Modifier.size(120.dp).background(Color.Magenta).testTag("pages"),
                entryProvider = { key -> NavEntry<NavKey>(key) {
                    Box(Modifier.fillMaxSize().background(if (key == ScreenKey.Home) Color.Black else Color.White))
                } },
            )
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
    }

    private fun scanline(): List<Int> {
        val bitmap = rule.onNodeWithTag("pages").captureToImage().asAndroidBitmap()
        return List(bitmap.width) { bitmap.getPixel(it, bitmap.height / 2) }
    }

    private fun frame() {
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
    }

    @Test fun tabFadeProgressesEvenlyIncludingMidFlightReversal() {
        launch()
        val samples = mutableListOf(red(scanline().first()))
        fun sample(count: Int) = repeat(count) { frame(); samples += red(scanline().first()) }
        rule.runOnIdle { nav.selectTab(ScreenKey.More) }
        sample(7)
        val atReversal = samples.lastIndex
        rule.runOnIdle { nav.selectTab(ScreenKey.Home) }
        sample(20)
        assertEquals(0, samples.first())
        assertTrue("must reverse while blended: $samples", samples[atReversal] in 40..225)
        assertTrue("single frame flash: $samples", samples.zipWithNext().all { (a, b) -> abs(b - a) <= 22 })
        assertEquals(0, samples.last())
    }

    @Test fun repeatedTabTapsSettleOnLastTabWithoutFlashing() {
        launch()
        rule.runOnIdle { nav.selectTab(ScreenKey.More) }
        repeat(26) { frame() }
        val samples = mutableListOf(red(scanline().first()))
        for ((tab, frames) in listOf(ScreenKey.Home to 12, ScreenKey.More to 4, ScreenKey.Home to 6, ScreenKey.More to 26)) {
            rule.runOnIdle { nav.selectTab(tab) }
            repeat(frames) { frame(); samples += red(scanline().first()) }
        }
        assertEquals(255, samples.first())
        assertTrue("taps had no visible response: $samples", samples.min() < 170)
        assertTrue("rapid taps flashed: $samples", samples.zipWithNext().all { (a, b) -> abs(b - a) <= 22 })
        assertEquals(255, samples.last())
    }

    private fun preview(edge: Int) {
        rule.runOnIdle {
            val dispatcher = rule.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, edge))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, 0.5f, edge))
        }
        repeat(5) { frame() }
    }

    @Test fun predictiveTabBackCanCancelThenCommit() {
        launch()
        rule.runOnIdle { nav.selectTab(ScreenKey.More) }
        repeat(24) { frame() }
        preview(BackEventCompat.EDGE_LEFT)
        assertTrue("halfway tab preview", red(scanline().first()) in 120..135)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        repeat(24) { frame() }
        assertEquals(255, red(scanline().first()))
        preview(BackEventCompat.EDGE_LEFT)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        repeat(24) { frame() }
        assertEquals(0, red(scanline().first()))
    }

    @Test fun predictiveBackTakesOverAnUnfinishedTabFadeWithoutJumpingToWhite() {
        launch()
        rule.runOnIdle { nav.selectTab(ScreenKey.More) }
        repeat(6) { frame() }
        val before = red(scanline().first())
        assertTrue(before in 50..150)
        preview(BackEventCompat.EDGE_LEFT)
        assertTrue("halfway gesture must start from the current blend", abs(red(scanline().first()) - before / 2) <= 12)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        repeat(24) { frame() }
        assertEquals(255, red(scanline().first()))
    }

    @Test fun childBackPreviewFollowsTheTouchedEdgeAndCancelKeepsThePage() {
        launch()
        rule.runOnIdle { nav.open(ScreenKey.Search) }
        repeat(24) { frame() }
        for (edge in listOf(BackEventCompat.EDGE_LEFT, BackEventCompat.EDGE_RIGHT)) {
            preview(edge)
            val pixels = scanline()
            val left = edge == BackEventCompat.EDGE_LEFT
            assertEquals(if (left) 0 else 255, red(pixels.first()))
            assertEquals(if (left) 255 else 0, red(pixels.last()))
            assertTrue("half swipe should reveal half the page", abs(pixels.count { red(it) == 0 } - pixels.size / 2) <= 2)
            rule.runOnIdle { rule.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
            repeat(26) { frame() }
            assertTrue(scanline().all { red(it) == 255 })
        }
    }

    @Test fun childPushAndPopRevealOpaquePagesAcrossAMovingEdge() {
        launch()
        for (push in listOf(true, false)) {
            rule.runOnIdle { if (push) nav.open(ScreenKey.Search) else nav.back() }
            val whiteWidths = List(26) {
                frame()
                val pixels = scanline()
                assertTrue("two pages blended (or backdrop exposed) on frame $it of push=$push", pixels.all {
                    it == android.graphics.Color.BLACK || it == android.graphics.Color.WHITE
                })
                pixels.count { it == android.graphics.Color.WHITE }
            }
            val width = scanline().size
            assertEquals(if (push) width else 0, whiteWidths.last())
            assertTrue("no continuous moving edge: $whiteWidths", whiteWidths.count { it in 1 until width } >= 6)
            assertTrue("edge jumped: $whiteWidths", whiteWidths.zipWithNext().all { (a, b) ->
                (if (push) b - a else a - b) in 0..(width / 8)
            })
        }
    }
}
