package com.ameermuawiya.apksigner.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp

/**
 * Reusable pull-to-refresh container rendering smooth Material 3 Expressive morphing loading spinner.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val pullState = rememberPullToRefreshState()
    val showIndicator = enabled && (isRefreshing || pullState.distanceFraction > 0.05f)
    val targetOffset = when {
        !enabled -> (-56).dp
        isRefreshing -> 20.dp
        pullState.distanceFraction > 0.05f -> ((pullState.distanceFraction * 60f) - 48f).coerceIn(-48f, 20f).dp
        else -> (-56).dp
    }
    val animatedOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ptrOffset"
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing && enabled,
        onRefresh = { if (enabled) onRefresh() },
        state = pullState,
        modifier = modifier.clipToBounds(),
        indicator = {
            if (showIndicator) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = animatedOffset),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveLoadingIndicator(
                            size = 28.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) {
        content()
    }
}
