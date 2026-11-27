package com.trackr.ui.screens.me

import org.junit.Assert.assertEquals

import com.trackr.core.model.CityChangeMode

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.trackr.core.designsystem.theme.TrackrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.graphics.asAndroidBitmap
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rCN-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MeCityChangeSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cityChangeOffersAskAutomaticAndManualChoices() {
        val events = mutableListOf<MeEvent>()
        compose.setContent { TrackrTheme { MeScreen(MeUiState(), { events += it }) } }
        compose.onNodeWithTag("me_settings_list").performScrollToNode(hasText("跨城切换"))
        compose.onNodeWithText("跨城切换").performClick()
        compose.onNode(hasText("询问后切换（默认）") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("自动切换").assertIsDisplayed()
        compose.onNodeWithText("保持当前城市").assertIsDisplayed()
        System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
            val file = java.io.File(directory, "city-change-settings.png")
            file.parentFile?.mkdirs()
            file.outputStream().use {
                compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.onNodeWithText("自动切换").performClick()
        assertEquals(listOf(MeEvent.SetCityChangeMode(CityChangeMode.Automatic)), events)
    }
}
