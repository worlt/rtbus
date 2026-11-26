package com.trackr.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Material's original two-title crossfade and scroll behavior, without an empty navigation row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingTitleBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {
    // Leave more room for very large accessibility text rather than clipping it.
    val crop = 40.dp * ((2f - LocalDensity.current.fontScale) / 0.5f).coerceIn(0f, 1f)
    fun expandedFraction() = 1f - scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    Box(modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets).clipToBounds()) {
        // The public two-row API uses the same native transition as LargeTopAppBar and lets
        // us keep the incoming small title centered within the remaining upper row.
        TwoRowsTopAppBar(
            title = { expanded ->
                if (expanded) {
                    ProvideTextStyle(MaterialTheme.typography.headlineMedium) {
                        Box(Modifier.layout { measurable, constraints ->
                            val content = measurable.measure(constraints)
                            val baseline = content[LastBaseline].takeIf { it != AlignmentLine.Unspecified } ?: 0
                            val padding = (28.dp.roundToPx() - (content.height - baseline))
                                .coerceIn(0, (56.dp.roundToPx() - content.height).coerceAtLeast(0))
                            layout(content.width, content.height + padding) { content.placeRelative(0, 0) }
                        }) { title() }
                    }
                } else {
                    Box(Modifier.layout { measurable, constraints ->
                        val content = measurable.measure(constraints)
                        val shift = (crop.toPx() * expandedFraction() / 2f).roundToInt()
                        layout(content.width, content.height) { content.placeRelative(0, shift) }
                    }) { title() }
                }
            },
            scrollBehavior = scrollBehavior,
            collapsedHeight = 64.dp,
            expandedHeight = 120.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = colors,
            modifier = Modifier.layout { measurable, constraints ->
                val toolbar = measurable.measure(constraints)
                val emptyRow = (crop.toPx() * expandedFraction()).roundToInt()
                layout(toolbar.width, (toolbar.height - emptyRow).coerceAtLeast(0)) {
                    toolbar.placeRelative(0, -emptyRow)
                }
            }
        )
    }
}
