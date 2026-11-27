package com.trackr.ui.screens.me

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.trackr.core.designsystem.theme.TrackrTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rCN-w360dp-h800dp")
class MeFavoritesBackupTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exportAndImportEntriesDispatchTheirOwnActions() {
        val events = mutableListOf<MeEvent>()
        compose.setContent { TrackrTheme { MeScreen(MeUiState(canTransferFavorites = true), { events += it }) } }
        val list = compose.onNodeWithTag("me_settings_list")
        list.performScrollToNode(hasText("导出收藏"))
        compose.onNodeWithText("导出收藏").assertIsDisplayed().performClick()
        list.performScrollToNode(hasText("导入收藏"))
        compose.onNodeWithText("导入收藏").assertIsDisplayed().performClick()
        assertEquals(listOf(MeEvent.ExportFavorites, MeEvent.ImportFavorites), events)
    }

    @Test fun unsupportedPlatformDoesNotOfferDeadFileActions() {
        compose.setContent { TrackrTheme { MeScreen(MeUiState(), {}) } }
        compose.onNodeWithText("导出收藏").assertDoesNotExist()
        compose.onNodeWithText("导入收藏").assertDoesNotExist()
    }

    @Test fun busyTransferPreventsDuplicateActions() {
        compose.setContent { TrackrTheme { MeScreen(MeUiState(canTransferFavorites = true, favoritesTransferBusy = true), {}) } }
        val list = compose.onNodeWithTag("me_settings_list")
        list.performScrollToNode(hasText("导出收藏"))
        compose.onNodeWithText("导出收藏").assertIsNotEnabled()
        list.performScrollToNode(hasText("导入收藏"))
        compose.onNodeWithText("导入收藏").assertIsNotEnabled()
    }
}
