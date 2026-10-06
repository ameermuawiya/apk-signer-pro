package com.ameermuawiya.apksigner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Full-screen loading overlay rendering only the centered ExpressiveLoadingIndicator without any text or labels.
 */
@Composable
fun FullScreenLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveLoadingIndicator(
                size = 64.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
