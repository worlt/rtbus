package com.trackr.ui.screens.me

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.trackr.core.designsystem.theme.TrackrTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rCN-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MeHeaderLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    @Config(qualifiers = "zh-rCN-w320dp-h640dp")
    fun originalMeHeaderFitsSmallPhoneWithLargeText() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, fontScale = 1.5f)) {
                TrackrTheme { MeScreen(MeUiState(), {}) }
            }
        }
        compose.onNodeWithText("我的").assertIsDisplayed()
        compose.onNodeWithText("显示").assertIsDisplayed()
        compose.onNodeWithText("长辈版").assertIsDisplayed()
    }

    @Test fun originalHeaderTransitionKeepsComfortableSpacingWhenReturningToTop() {
        compose.setContent { TrackrTheme(darkTheme = true) { MeScreen(MeUiState(), {}) } }
        fun headerHeight() = compose.onNodeWithTag("me_top_bar").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        fun assertGroupGap() {
            val header = compose.onNodeWithTag("me_top_bar").getUnclippedBoundsInRoot()
            val group = compose.onNodeWithText("显示").getUnclippedBoundsInRoot()
            assertTrue("首组标题与顶栏保持呼吸空间", group.top - header.bottom >= 16.dp)
            assertTrue("首组不再额外撑开大片空白", group.top - header.bottom <= 32.dp)
            compose.onNodeWithText("我的").assertIsDisplayed()
        }
        fun capture(name: String) {
            System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
                val file = java.io.File(directory, name)
                file.parentFile?.mkdirs()
                file.outputStream().use {
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        val expanded = headerHeight()
        assertTrue("缩短展开区，保留大标题", expanded in 80.dp..112.dp)
        val expandedHeader = compose.onNodeWithTag("me_top_bar").getUnclippedBoundsInRoot()
        val expandedTitle = compose.onNodeWithText("我的").getUnclippedBoundsInRoot()
        val titleTopGap = expandedTitle.top - expandedHeader.top - (expanded - 80.dp)
        val titleBottomGap = expandedHeader.bottom - expandedTitle.bottom
        assertTrue("展开标题上下留白应接近：$titleTopGap / $titleBottomGap",
            kotlin.math.abs((titleTopGap - titleBottomGap).value) <= 16f)
        assertGroupGap()
        capture("original-me-expanded-dark.png")
        repeat(2) { cycle ->
            compose.onNodeWithTag("me_settings_list").performTouchInput { swipeUp() }
            compose.onNodeWithTag("me_settings_list").performScrollToIndex(0)
            org.junit.Assert.assertEquals("上滑仍保留原版收起过渡", 16f, (expanded - headerHeight()).value, 1f)
            assertGroupGap()
            if (cycle == 0) capture("original-me-collapsed-dark.png")
            compose.onNodeWithTag("me_settings_list").performTouchInput { swipeDown() }
            org.junit.Assert.assertEquals("回顶重新展开，不能累积额外留白", expanded.value, headerHeight().value, 1f)
            assertGroupGap()
        }
    }
}
