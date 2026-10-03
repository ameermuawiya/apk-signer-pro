package com.ameermuawiya.apksigner.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Indicates vertical position within grouped card layouts.
 */
enum class CardGroupPosition { FIRST, MIDDLE, LAST, SINGLE }

/**
 * Generates rounded corner shapes tailored to group positions.
 */
fun getGroupedCardShape(position: CardGroupPosition): Shape {
    return when (position) {
        CardGroupPosition.FIRST -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        CardGroupPosition.MIDDLE -> RoundedCornerShape(4.dp)
        CardGroupPosition.LAST -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        CardGroupPosition.SINGLE -> RoundedCornerShape(20.dp)
    }
}
