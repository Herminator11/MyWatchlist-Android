package com.example.mywatchlist.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mywatchlist.MovieEntity
import com.example.mywatchlist.ui.theme.AppTheme

// ─────────────────────────────────────────────
// SWIPEABLE WRAPPER
// Swipe rechts = Bearbeiten (onSwipe)
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableMediaCard(
    item: MovieEntity,
    onSwipe: () -> Unit,
    cardContent: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipe()
            }
            false
        },
        positionalThreshold = { it * 0.3f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = { EditSwipeBackground(dismissState) },
        content = { cardContent() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSwipeBackground(dismissState: SwipeToDismissBoxState) {
    val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd

    val backgroundColor by animateColorAsState(
        targetValue = if (isSwiping) AppTheme.YellowAccent else AppTheme.DarkSurface2,
        label = "swipe_bg_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Bearbeiten",
            tint = if (isSwiping) Color.Black else AppTheme.TextSecondary,
            modifier = Modifier
                .padding(start = 24.dp)
                .size(26.dp)
        )
    }
}