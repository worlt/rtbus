package com.trackr.feature.bus.home

import androidx.lifecycle.compose.rememberLifecycleOwner

import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.lifecycle.Lifecycle

import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.trackr.core.designsystem.theme.TrackrTheme
import com.trackr.core.model.TransitCity
import com.trackr.core.model.UiMode
import com.trackr.feature.bus.elder.ElderBusHomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rCN-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CityChangeUiTest {
    @get:Rule val compose = createComposeRule()
    private val shanghai = TransitCity(cityId = "034", cityName = "上海")
    private val hangzhou = TransitCity(cityId = "0571", cityName = "杭州")

    @Test fun inactiveHomeDoesNotShowDialogOrTreatHidingAsCancellation() {
        val maxLifecycle = mutableStateOf(Lifecycle.State.CREATED)
        val state = BusHomeUiState(currentCity = shanghai, locatedCity = hangzhou, pendingCityChange = hangzhou)
        val events = mutableListOf<BusHomeEvent>()
        compose.setContent {
            val owner = rememberLifecycleOwner(maxLifecycle = maxLifecycle.value)
            androidx.compose.runtime.CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TrackrTheme { CityChangeConfirmation(state, { events += it }) }
            }
        }
        compose.onNodeWithText("切换到定位城市？").assertDoesNotExist()
        compose.runOnIdle { maxLifecycle.value = Lifecycle.State.RESUMED }
        compose.onNodeWithText("切换到定位城市？").assertIsDisplayed()
        compose.runOnIdle { maxLifecycle.value = Lifecycle.State.CREATED }
        compose.onNodeWithText("切换到定位城市？").assertDoesNotExist()
        assertEquals(emptyList<BusHomeEvent>(), events)
    }

    @Test fun confirmSwitchesOnlyAfterUserAccepts() {
        val state = mutableStateOf(BusHomeUiState(currentCity = shanghai, locatedCity = hangzhou, pendingCityChange = hangzhou))
        val events = mutableListOf<BusHomeEvent>()
        compose.setContent { TrackrTheme { BusHomeScreen(state.value, { event ->
            events += event
            if (event == BusHomeEvent.ConfirmCityChange) state.value = state.value.copy(currentCity = hangzhou, pendingCityChange = null)
        }) } }
        compose.onNodeWithTag(BusHomeTags.CITY_TITLE).assertTextContains("上海")
        compose.onNodeWithText("切换到定位城市？").assertIsDisplayed()
        System.getenv("RTBUS_SCREENSHOT_DIR")?.let { directory ->
            val file = java.io.File(directory, "city-change-confirmation.png")
            file.parentFile?.mkdirs()
            file.outputStream().use {
                compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.onNodeWithText("确定").performClick()
        compose.onNodeWithText("切换到定位城市？").assertDoesNotExist()
        compose.onNodeWithTag(BusHomeTags.CITY_TITLE).assertTextContains("杭州")
        assertEquals(listOf(BusHomeEvent.ConfirmCityChange), events)
    }

    @Test fun elderHomeAlsoOffersCancelAndKeepsSelectedCity() {
        val state = mutableStateOf(BusHomeUiState(currentCity = shanghai, locatedCity = hangzhou, pendingCityChange = hangzhou))
        val events = mutableListOf<BusHomeEvent>()
        compose.setContent { TrackrTheme(uiMode = UiMode.Elder) { ElderBusHomeScreen(state.value, { event ->
            events += event
            if (event == BusHomeEvent.CancelCityChange) state.value = state.value.copy(pendingCityChange = null)
        }) } }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("切换到定位城市？").assertDoesNotExist()
        compose.onNodeWithText("上海").assertIsDisplayed()
        assertEquals(shanghai, state.value.currentCity)
        assertEquals(listOf(BusHomeEvent.CancelCityChange), events)
    }
}
