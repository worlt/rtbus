package com.trackr.feature.bus.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import com.trackr.core.designsystem.resources.Res
import com.trackr.core.designsystem.resources.ic_chevron_right
import com.trackr.feature.bus.resources.Res as BusRes
import com.trackr.feature.bus.resources.bus_home_back_to_top
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** 避免在页首轻微滚动就出现；单张长卡片滚过一屏时也能返回。 */
@Composable
fun rememberShowHomeBackToTop(listState: LazyListState): State<Boolean> = remember(listState) {
    derivedStateOf {
        val viewport = listState.layoutInfo.viewportSize.height
        listState.firstVisibleItemIndex >= 3 ||
            (viewport > 0 && listState.firstVisibleItemScrollOffset >= viewport)
    }
}

@Composable
fun HomeBackToTopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.testTag("bus_home_back_to_top")
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_chevron_right),
            contentDescription = stringResource(BusRes.string.bus_home_back_to_top),
            modifier = Modifier.rotate(-90f)
        )
    }
}
