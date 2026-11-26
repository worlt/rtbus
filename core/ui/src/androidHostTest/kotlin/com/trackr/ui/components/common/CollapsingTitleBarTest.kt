package com.trackr.ui.components.common

import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.trackr.core.designsystem.theme.TrackrTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rCN-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CollapsingTitleBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keepsTitleVisibleAcrossNativeTransitionWithLargeText() = verifyTransition(1.5f)

    @Test fun leavesRoomForDoubleSizeAccessibilityText() = verifyTransition(2f)

    private fun verifyTransition(fontScale: Float) {
        lateinit var behavior: TopAppBarScrollBehavior
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                TrackrTheme(darkTheme = true) {
                    behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                    CollapsingTitleBar(title = { Text("清远") }, scrollBehavior = behavior,
                        modifier = Modifier.testTag("title_bar"))
                }
            }
        }
        for (fraction in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            compose.runOnIdle { behavior.state.heightOffset = behavior.state.heightOffsetLimit * fraction }
            compose.onNodeWithText("清远").assertIsDisplayed()
            val title = compose.onNodeWithText("清远").getUnclippedBoundsInRoot()
            val bar = compose.onNodeWithTag("title_bar").getUnclippedBoundsInRoot()
            if (fraction == 0f || fraction == 0.5f || fraction == 1f) {
                System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
                    val file = java.io.File(directory, "balanced-title-font-$fontScale-fraction-$fraction.png")
                    file.parentFile?.mkdirs()
                    file.outputStream().use {
                        compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
            }
            assertTrue("标题不能越过状态栏区域：fraction=$fraction title=$title bar=$bar", title.top >= bar.top)
        }
    }
}
