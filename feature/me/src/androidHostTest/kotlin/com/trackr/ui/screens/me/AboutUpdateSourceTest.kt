package com.trackr.ui.screens.me

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.trackr.core.designsystem.theme.TrackrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1600dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutUpdateSourceTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun projectLinks_openRtbusAndUpstreamSeparately() {
        val openedUrls = mutableListOf<String>()
        val uriHandler = object : UriHandler {
            override fun openUri(uri: String) {
                openedUrls += uri
            }
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                TrackrTheme {
                    AboutScreen(state = AboutUiState(), onEvent = {})
                }
            }
        }

        composeTestRule.onNodeWithText("本项目").assertIsDisplayed()
        composeTestRule.onNodeWithText("worlt/rtbus").performScrollTo().performClick()
        composeTestRule.onNodeWithText("上游项目").assertIsDisplayed()
        composeTestRule.onNodeWithText("5151561/Trackr").performScrollTo().performClick()

        assertEquals(
            listOf("https://github.com/worlt/rtbus", "https://github.com/5151561/Trackr"),
            openedUrls
        )
    }

    @Test
    fun unconfiguredSource_showsExplanationAndDismissesWithoutOfferingInstall() {
        val events = mutableListOf<AboutEvent>()
        composeTestRule.setContent {
            TrackrTheme {
                AboutScreen(
                    state = AboutUiState(canCheckUpdates = true, update = AboutUpdateUiState.NotConfigured),
                    onEvent = { events += it }
                )
            }
        }
        composeTestRule.onNode(hasText("更新源未配置") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        composeTestRule.onNodeWithText("当前版本尚未配置实时公交的更新源，暂时无法检查新版本。").assertIsDisplayed()
        composeTestRule.onNodeWithText("下载并安装").assertDoesNotExist()
        composeTestRule.onNodeWithText("已是最新版本").assertDoesNotExist()
        composeTestRule.onNodeWithText("知道了").performClick()
        assertEquals(listOf(AboutEvent.DismissUpdateDialog), events)
    }
}
